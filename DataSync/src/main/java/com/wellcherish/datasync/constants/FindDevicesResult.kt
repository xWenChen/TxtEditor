package com.wellcherish.datasync.constants

sealed class FindDevicesResult(
    /**
     * 失败时的错误码，成功时该值没用
     * */
    var errorCode: Int,
) {
    class Success : FindDevicesResult(0)
    class Error(errorCode: Int) : FindDevicesResult(errorCode)
}