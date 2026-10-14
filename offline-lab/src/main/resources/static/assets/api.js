export class ApiError extends Error {
    constructor(message, status, code, traceId) {
        super(message);
        this.status = status;
        this.code = code;
        this.traceId = traceId;
    }
}

export function createApi(token) {
    return async (path, {method = 'GET', data, signal} = {}) => {
        const headers = {'X-Lab-Token': token()};
        if (data !== undefined) headers['Content-Type'] = 'application/json';
        const response = await fetch(path, {
            method,
            headers,
            body: data === undefined ? undefined : JSON.stringify(data),
            signal
        });
        let result;
        try {
            result = await response.json();
        } catch {
            throw new ApiError('服务返回了无效响应，请稍后重试', response.status);
        }
        if (!response.ok || !result.success) throw new ApiError(result.error || '请求失败', response.status, result.code, result.traceId);
        return result.data;
    };
}

export function showError(element, error) {
    element.className = 'status error';
    element.textContent = error.message + (error.traceId ? `（请求编号：${error.traceId}）` : '');
}
