package cn.lsz.rocketmqlistener.init;

import cn.lsz.rocketmqlistener.annotation.RocketListener;
import cn.lsz.rocketmqlistener.config.MqProperties;
import cn.lsz.rocketmqlistener.entity.MethodInvokeObject;
import cn.lsz.rocketmqlistener.listener.RocketMessageListener;
import cn.lsz.rocketmqlistener.util.RocketListenerAnnotationUtils;
import cn.lsz.rocketmqlistener.util.TagUtils;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.MessageListener;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.bean.ConsumerBean;
import com.aliyun.openservices.ons.api.bean.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.el.MethodNotFoundException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * description
 *
 * @author LSZ 2022/07/19 17:52
 * @contact 648748030@qq.com
 */
@Component
public class RocketBeanPostProcessor implements BeanPostProcessor, Ordered, SmartInitializingSingleton, DisposableBean {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MqProperties mqProperties;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RocketListenerAnnotationUtils rocketListenerAnnotationUtils;

    private List<ConsumerBean> list = new ArrayList<>();

    /**
     * 存放事件的订阅关系缓存
     * map<GID, <TOPIC, map<TAG, List<监听方法>>>>
     */
    private static Map<String, Map<String, Map<String, List<MethodInvokeObject>>>> CACHE = new HashMap<>();

    private static Map<String, Properties> CACHE_PROPERTIES = new HashMap<>();

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        Map<Method, Set<RocketListener>> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
                (MethodIntrospector.MetadataLookup<Set<RocketListener>>) method -> {
                    Set<RocketListener> listenerMethods = findListenerAnnotations(method);
                    return (!listenerMethods.isEmpty() ? listenerMethods : null);
                });

        for (Map.Entry<Method, Set<RocketListener>> entry : annotatedMethods.entrySet()) {
            Method method = entry.getKey();
            for (RocketListener annotation : entry.getValue()) {
                //是否不需要启动
                ConditionalOnProperty conditional = AnnotationUtils.findAnnotation(method, ConditionalOnProperty.class);
                if(conditional != null){
                    String propertyName = conditional.name()[0];
                    String propertyValue = applicationContext.getEnvironment().getProperty(propertyName);
                    if(propertyValue == null && !conditional.matchIfMissing()){
                        continue;
                    }
                    if(propertyValue != null && !propertyValue.equals(conditional.havingValue())){
                        continue;
                    }
                }

                String topic = annotation.topic();
                String gid = annotation.gid();
                String properties = annotation.properties();
                //是否需要排序
                Integer sort = null;
                Order order = AnnotationUtils.findAnnotation(method, Order.class);
                if(order != null){
                    sort = order.value();
                }
                for (String tag : annotation.tags()) {
                    add(gid, topic, tag, new MethodInvokeObject(bean, method, sort));
                }
                //properties配置
                if(properties != null){
                    addProperties(gid, properties);
                }

            }
        }
        return bean;
    }

    private Set<RocketListener> findListenerAnnotations(Method method) {
        Set<RocketListener> listeners = new HashSet<>();
        RocketListener ann = AnnotatedElementUtils.findMergedAnnotation(method, RocketListener.class);
        if (ann != null) {
            listeners.add(ann);
        }
        return listeners;
    }

    private void addProperties(String gid, String propertiesBeanName){
        if(!applicationContext.containsBean(propertiesBeanName)){
            throw new RuntimeException("RocketListener不存在的配置bean："+propertiesBeanName);
        }
        Properties propertiesBean = applicationContext.getBean(propertiesBeanName, Properties.class);
        if(propertiesBean == null){
            throw new RuntimeException("RocketListener不存在的配置bean："+propertiesBeanName);
        }

        Properties properties = CACHE_PROPERTIES.get(gid);
        if(properties == null){
            CACHE_PROPERTIES.put(gid, propertiesBean);
        }else {
            if(!propertiesBean.equals(properties)){
                throw new RuntimeException("RocketListener gid: " + gid + "存在不同配置bean："+propertiesBeanName);
            }
        }
    }

    private Properties getGidProperties(String gid){
        return CACHE_PROPERTIES.get(gid);
    }

    public static Set<String> getGidSet(){
        Set<String> set = CACHE.keySet();
        return set;
    }

    public static Set<String> getTopicSet(String gid){
        Map<String, Map<String, List<MethodInvokeObject>>> gidMap = CACHE.get(gid);
        if(gidMap == null){
            return null;
        }
        return gidMap.keySet();
    }

    public static Set<String> getTagSet(String gid, String topic){

        Map<String, Map<String, List<MethodInvokeObject>>> gidMap = CACHE.get(gid);
        if(gidMap == null){
            return null;
        }
        Map<String, List<MethodInvokeObject>> tagMap = gidMap.get(topic);
        if(tagMap == null){
            return null;
        }
        return tagMap.keySet();
    }

    public static List<MethodInvokeObject> getTagEventInvokeObjects(String gid, String topic, String tag){
        Map<String, Map<String, List<MethodInvokeObject>>> gidMap = CACHE.get(gid);
        if(gidMap == null){
            return null;
        }
        Map<String, List<MethodInvokeObject>> tagMap = gidMap.get(topic);
        if(tagMap == null){
            return null;
        }
        List<MethodInvokeObject> result = tagMap.get(tag);
        return result;
    }

    public static List<MethodInvokeObject> getAllTagEventInvokeObjects(String gid, String topic){
        Map<String, Map<String, List<MethodInvokeObject>>> gidMap = CACHE.get(gid);
        if(gidMap == null){
            return null;
        }
        Map<String, List<MethodInvokeObject>> tagMap = gidMap.get(gid);
        if(tagMap == null){
            return null;
        }
        List<MethodInvokeObject> result = tagMap.get("*");
        return result;
    }

    private void add(String gid, String topic, String tag, MethodInvokeObject eventInvokeObject){
        //校验参数类型是否合法
        validateParameterTypes(eventInvokeObject);
        gid = rocketListenerAnnotationUtils.replaceValue(gid);
        topic = rocketListenerAnnotationUtils.replaceValue(topic);

        Map<String, Map<String, List<MethodInvokeObject>>> gidMap = CACHE.get(gid);
        if(gidMap == null){
            gidMap = new HashMap<>();
            CACHE.put(gid, gidMap);
        }
        Map<String, List<MethodInvokeObject>> topicMap = gidMap.get(topic);
        if(topicMap == null){
            topicMap = new HashMap<>();
            gidMap.put(topic, topicMap);
        }

        List<MethodInvokeObject> list = topicMap.get(tag);
        if(list == null){
            list = new CopyOnWriteArrayList<>();
            topicMap.put(tag, list);
        }
        list.add(eventInvokeObject);
        //排序
        list.sort((v1, v2) -> {
            Integer sort1 = v1.getSort();
            Integer sort2 = v2.getSort();

            if(sort1 != null && sort2 != null){
                return sort1.compareTo(sort2);
            }
            if(sort1 == null){
                return 1;
            }
            if(sort2 == null){
                return -1;
            }
            return 0;
        });
        LOGGER.info("ECC init add {}:{}:{}", gid,topic,tag);
    }

    /**
     * 校验方法限制在已知参数范围内
     * @param eventInvokeObject
     */
    private void validateParameterTypes(MethodInvokeObject eventInvokeObject){
        Object bean = eventInvokeObject.getBean();
        Method method = eventInvokeObject.getMethod();
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (Class<?> parameterType : parameterTypes) {
/*            if(parameterType == Event.class){
                continue;
            }*/
            if(parameterType == Message.class){
                continue;
            }
            throw new MethodNotFoundException("方法"+ bean.getClass().getName() + "." + method.getName() +"不支持的参数类型:" + parameterType.getName());
        }
    }


    /**
     *
     * @return
     */
    public ConsumerBean eventCenterClusteringConsumerBean(String gid, Set<String> topics) {
       /* String suffix = mqProperties.getSuffix();
        String groupId = gid + suffix;*/
        String groupId = gid;

        ConsumerBean consumerBean = new ConsumerBean();
        //配置文件
        Properties properties = mqProperties.getPropertie();
        properties.setProperty(PropertyKeyConst.GROUP_ID, groupId);

        Properties gidProperties = getGidProperties(gid);
        if(gidProperties != null){
            properties.putAll(gidProperties);
        }

        consumerBean.setProperties(properties);
        //订阅关系
        Map<Subscription, MessageListener> subscriptionTable = new HashMap<>();
        RocketMessageListener rocketMessageListener = new RocketMessageListener(gid);
        for (String topic : topics) {
            Set<String> tag = RocketBeanPostProcessor.getTagSet(gid, topic);
            if(tag == null || tag.isEmpty()){
                continue;
            }
            //tag排序，避免aliyun因顺序错乱抛出订阅关系不一致错误
            List sortTag = tag.stream().sorted().collect(Collectors.toList());
            String tags = TagUtils.join(sortTag);
            //项目任务队列
            Subscription subscription = new Subscription();
            subscription.setTopic(topic);
            subscription.setExpression(tags);
            subscriptionTable.put(subscription, rocketMessageListener);
        }

        consumerBean.setSubscriptionTable(subscriptionTable);
        return consumerBean;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Set<String> gidSet = RocketBeanPostProcessor.getGidSet();
        if(gidSet != null) {
            for (String gid : gidSet) {
                Set<String> topicSet = RocketBeanPostProcessor.getTopicSet(gid);
                ConsumerBean consumerBean = eventCenterClusteringConsumerBean(gid, topicSet);
                list.add(consumerBean);
                consumerBean.start();
            }
        }
    }

    @Override
    public void destroy() {
        for (ConsumerBean consumerBean : list) {
            consumerBean.shutdown();
        }
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

}
