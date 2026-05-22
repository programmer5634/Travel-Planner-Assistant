export function parseApiError(error: unknown) {
  if (typeof error === 'object' && error !== null) {
    const maybeAxiosError = error as {
      code?: string;
      message?: string;
      response?: { data?: { error?: string } };
    };

    if (maybeAxiosError.response?.data?.error) {
      return maybeAxiosError.response.data.error;
    }

    if (maybeAxiosError.code === 'ECONNABORTED') {
      return '请求超时，请稍后重试';
    }

    if (maybeAxiosError.message) {
      return maybeAxiosError.message;
    }
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return '请求失败，请稍后重试';
}
