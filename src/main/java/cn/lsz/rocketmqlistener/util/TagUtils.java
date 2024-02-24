package cn.lsz.rocketmqlistener.util;

import java.util.Collection;

/**
 * description
 *
 * @author LSZ 2022/12/07 18:01
 * @contact 648748030@qq.com
 */
public class TagUtils {

    private static final String SEPARATOR = "||";

    public static String join(Collection<String> tags){
        StringBuilder result = new StringBuilder();
        for (String tag : tags) {
            result.append(SEPARATOR).append(tag);
        }
        result.delete(0, SEPARATOR.length());

        return result.toString();
    }

}
