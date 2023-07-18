package com.tuochexia.manualsync.service;

/**
 * @author linx
 * @version 1.0.0
 * @date 2023-07-12 10:48:00
 * @describe 通过execl模板把向下付款的应收、应付同步到系统中
 */
public interface IPayDown {
    long syncPayDown();
}
