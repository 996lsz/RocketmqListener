package cn.lsz.rocketmqlistener.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * description
 *
 * @author LSZ 2023/06/29 15:05
 * @contact 648748030@qq.com
 */
@Service
public class RocketListenerAnnotationUtils {

    @Autowired
    private Environment environment;

    private static Pattern PATTERN = Pattern.compile("\\$\\{[^}]+}");

    public String replaceValue(String str){
        for(;;){
            Matcher m = PATTERN.matcher(str);
            if (m.find()) {
                String group = m.group();
                str = m.replaceFirst(replacePropertiesValue(group));
            }else {
                break;
            }
        }
        return str;
    }

    private String replacePropertiesValue(String arg){
        if (arg.startsWith("${") && arg.endsWith("}")) {
            String propertyName = arg.substring(2, arg.length() - 1);
            //判断是否有默认值
            String[] split = propertyName.split(":");
            arg = environment.getProperty(split[0]);
            if(arg == null && split.length == 2){
                arg = split[1];
            }
        }
        return arg == null ? "" : arg;
    }
}
