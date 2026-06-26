<script>
export default {
  onLaunch() {
    const token = uni.getStorageSync('token')
    if (!token) {
      uni.reLaunch({ url: '/pages/login/login' })
    } else {
      uni.switchTab({ url: '/pages/index/index' })
    }
  }
}
</script>

<style>
page { background: #f5f6fa; font-size: 28rpx; color: #333; }
.container { padding: 24rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 20rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,.04); }
.btn-primary { background: #2979ff; color: #fff; border-radius: 12rpx; text-align: center; padding: 20rpx; }
.btn-outline { border: 1rpx solid #2979ff; color: #2979ff; border-radius: 12rpx; text-align: center; padding: 20rpx; }
.btn-disabled { background: #e8e8e8; color: #999; border-radius: 12rpx; text-align: center; padding: 20rpx; }
.text-muted { color: #999; font-size: 24rpx; }
.tag { display: inline-block; padding: 4rpx 16rpx; background: #eef3ff; color: #2979ff; border-radius: 8rpx; font-size: 22rpx; margin-right: 8rpx; }
</style>
