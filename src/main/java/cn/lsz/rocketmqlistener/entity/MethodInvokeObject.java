package cn.lsz.rocketmqlistener.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;

/**
 * 存放执行方法对象
 *
 * @author LSZ 2022/07/19 18:00
 * @contact 648748030@qq.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodInvokeObject {

    private Object bean;

    private Method method;

    private Integer sort;


}
