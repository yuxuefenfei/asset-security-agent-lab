package lab.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.Flow;

/** 在响应接收过程中执行容量上限，取消超限下载。 */
final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<String> {
    private final int limit;
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final CompletableFuture<String> body = new CompletableFuture<>();
    private Flow.Subscription subscription;

    LimitedBodySubscriber(int limit) {
        this.limit = limit;
    }

    @Override
    public CompletionStage<String> getBody() {
        return body;
    }

    @Override
    public void onSubscribe(Flow.Subscription value) {
        subscription = value;
        // 每次只申请一批数据，处理完再继续，避免无节制地缓存上游响应。
        value.request(1);
    }

    @Override
    public void onNext(List<ByteBuffer> buffers) {
        for (var buffer : buffers) {
            // 在分配并写入当前块之前检查剩余额度，超限立即取消订阅。
            if (buffer.remaining() > limit - bytes.size()) {
                subscription.cancel();
                body.completeExceptionally(new IOException("模型响应超过容量上限"));
                return;
            }
            byte[] chunk = new byte[buffer.remaining()];
            buffer.get(chunk);
            bytes.writeBytes(chunk);
        }
        subscription.request(1);
    }

    @Override
    public void onError(Throwable error) {
        body.completeExceptionally(error);
    }

    @Override
    public void onComplete() {
        body.complete(bytes.toString(StandardCharsets.UTF_8));
    }
}
