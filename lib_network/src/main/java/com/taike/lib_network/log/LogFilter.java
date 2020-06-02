package com.taike.lib_network.log;

/**
 * 过滤log的算法接口
 *
 * @author LN
 */
public interface LogFilter {
    /**
     *
     * @param log 输入的log
     * @return 过滤后的log
     */
    String filter(String log);
}
