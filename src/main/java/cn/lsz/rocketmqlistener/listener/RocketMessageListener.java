package cn.lsz.rocketmqlistener.listener;

import cn.lsz.rocketmqlistener.entity.MethodInvokeObject;
import cn.lsz.rocketmqlistener.init.RocketBeanPostProcessor;
import com.aliyun.openservices.ons.api.Action;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


/**
 * @description: 处理任务队列
 * @author: lsz
 */
public class RocketMessageListener implements MessageListener {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    public String gid;

/*    public String suffix;

    public RocketMessageListener(String gid, String suffix){
        this.gid = gid;
        this.suffix = suffix;
    }*/
    public RocketMessageListener(String gid){
        this.gid = gid;
    }

    @Override
    public Action consume(Message message, ConsumeContext context) {
        List<MethodInvokeObject> tagEventInvokeObjects = null;
        //Event event = null;
        try {
            String tag = message.getTag();
            String topic = message.getTopic();
            //topic = topic.substring(0, topic.length() - suffix.length());
            tagEventInvokeObjects = RocketBeanPostProcessor.getTagEventInvokeObjects(gid, topic, tag);
            //全量监听的方法
            List<MethodInvokeObject> allTagEventInvokeObjects = RocketBeanPostProcessor.getAllTagEventInvokeObjects(gid, topic);
            if(allTagEventInvokeObjects == null){
                allTagEventInvokeObjects = new ArrayList<>();
            }
            tagEventInvokeObjects.addAll(allTagEventInvokeObjects);

            //String s = new String(message.getBody());
            //event = JSONObject.parseObject(s, Event.class);
        }catch (Exception e){
            LOGGER.error("ECC内部错误,请联系ECC开发人员诊断,msgid="+message.getMsgID(), e);
            return Action.CommitMessage;
        }

        try {
            for (MethodInvokeObject tagEventInvokeObject : tagEventInvokeObjects) {

                Object bean = tagEventInvokeObject.getBean();
                Method method = tagEventInvokeObject.getMethod();
                //适配参数类型
                Class<?>[] parameterTypes = method.getParameterTypes();
                Object args[] = new Object[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    Class<?> parameterType = parameterTypes[i];
/*                    if(parameterType == Event.class){
                        args[i] = event;
                        continue;
                    }*/
                    if(parameterType == Message.class){
                        args[i] = message;
                        continue;
                    }
                    args[i] = null;
                }
                Object invoke = method.invoke(bean, args);
                if(invoke != null && invoke.getClass() == Action.class){
                    Action action = (Action) invoke;
                    if(action == Action.ReconsumeLater){
                        //手动变更了Action则重试
                        return Action.ReconsumeLater;
                    }
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return Action.CommitMessage;
    }

}
