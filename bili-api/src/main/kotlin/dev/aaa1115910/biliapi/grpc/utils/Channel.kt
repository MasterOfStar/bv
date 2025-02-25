package dev.aaa1115910.biliapi.grpc.utils

import bilibili.metadata.device.device
import bilibili.metadata.locale.locale
import bilibili.metadata.metadata
import bilibili.metadata.network.NetworkType
import bilibili.metadata.network.network
import dev.aaa1115910.biliapi.http.util.BiliAppConf
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.MethodDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import io.grpc.Metadata as GrpcMetadata

fun generateChannel(
    accessKey: String,
    buvid: String
): ManagedChannel = ManagedChannelBuilder
    .forAddress(BiliAppConf.GRPC_HOST, BiliAppConf.GRPC_PORT)
    .useTransportSecurity()
    .executor(Dispatchers.IO.asExecutor())
    .intercept(MetadataInterceptor(accessKey, buvid))
    .build()

private class MetadataInterceptor(
    private val accessKey: String,
    private val buvid: String
) : ClientInterceptor {
    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>, callOptions: CallOptions, next: Channel
    ): ClientCall<ReqT, RespT> {
        return object : SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            override fun start(responseListener: Listener<RespT>, headers: GrpcMetadata) {
                headers.apply {
                    putAuthorization(accessKey)
                    putMetadataBin(accessKey, buvid)
                    putDeviceBin(buvid)
                    putLocalBin()
                    putNetworkBin()
                }
                super.start(responseListener, headers)
            }
        }
    }
}

fun GrpcMetadata.putAuthorization(accessKey: String) {
    put(
        GrpcMetadata.Key.of("authorization", GrpcMetadata.ASCII_STRING_MARSHALLER),
        "identify_v1 $accessKey"
    )
}

fun GrpcMetadata.putMetadataBin(accessKey: String, buvid: String) {
    put(
        GrpcMetadata.Key.of("x-bili-metadata-bin", GrpcMetadata.BINARY_BYTE_MARSHALLER),
        metadata {
            this.accessKey = accessKey
            mobiApp = BiliAppConf.MOBI_APP
            device = BiliAppConf.DEVICE
            build = BiliAppConf.APP_BUILD_CODE
            channel = BiliAppConf.CHANNEL
            this.buvid = buvid
            platform = BiliAppConf.PLATFORM
        }.toByteArray()
    )
}

fun GrpcMetadata.putDeviceBin(buvid: String) {
    put(
        io.grpc.Metadata.Key.of("x-bili-device-bin", GrpcMetadata.BINARY_BYTE_MARSHALLER),
        device {
            appId = BiliAppConf.APP_ID
            mobiApp = BiliAppConf.MOBI_APP
            device = BiliAppConf.DEVICE
            build = BiliAppConf.APP_BUILD_CODE
            channel = BiliAppConf.CHANNEL
            this.buvid = buvid
            platform = BiliAppConf.PLATFORM
        }.toByteArray()
    )
}

fun GrpcMetadata.putLocalBin() {
    put(
        io.grpc.Metadata.Key.of("x-bili-local-bin", GrpcMetadata.BINARY_BYTE_MARSHALLER),
        locale {
            timezone = BiliAppConf.TIMEZONE
        }.toByteArray()
    )
}

fun GrpcMetadata.putNetworkBin() {
    put(
        io.grpc.Metadata.Key.of("x-bili-network-bin", GrpcMetadata.BINARY_BYTE_MARSHALLER),
        network {
            type = NetworkType.WIFI
        }.toByteArray()
    )
}
