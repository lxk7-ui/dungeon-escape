package com.example.dungeonescape.persistence;

/**
 * 关卡加载或校验失败时抛出的异常。
 *
 * <p>同时携带来源（classpath 路径、文件名或调用方自定义名称）与具体原因，
 * 消息格式为“关卡加载失败（来源：…）：…”；{@link #getSource()} 与
 * {@link #getReason()} 可分别获取两个组成部分。
 */
public class InvalidLevelException extends RuntimeException {

    private final String source;
    private final String reason;

    /**
     * @param source 关卡来源（资源路径、文件路径或自定义名称）
     * @param reason 具体失败原因
     */
    public InvalidLevelException(String source, String reason) {
        super("关卡加载失败（来源：" + source + "）：" + reason);
        this.source = source;
        this.reason = reason;
    }

    /** 返回关卡来源（资源路径、文件路径或自定义名称）。 */
    public String getSource() {
        return source;
    }

    /** 返回具体失败原因。 */
    public String getReason() {
        return reason;
    }
}
