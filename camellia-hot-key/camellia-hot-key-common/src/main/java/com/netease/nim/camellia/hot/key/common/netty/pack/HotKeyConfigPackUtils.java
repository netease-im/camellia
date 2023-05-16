package com.netease.nim.camellia.hot.key.common.netty.pack;

import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.common.model.Rule;
import com.netease.nim.camellia.hot.key.common.model.RuleType;
import com.netease.nim.camellia.hot.key.common.netty.codec.ArrayMable;
import com.netease.nim.camellia.hot.key.common.netty.codec.Pack;
import com.netease.nim.camellia.hot.key.common.netty.codec.Props;
import com.netease.nim.camellia.hot.key.common.netty.codec.Unpack;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/5/8
 */
public class HotKeyConfigPackUtils {

    private enum NamespaceTag {
        namespace(1),
        ;

        private final int value;

        NamespaceTag(int value) {
            this.value = value;
        }
    }

    private enum RuleTag {
        name(1),
        type(2),
        keyConfig(3),
        checkMillis(4),
        checkThreshold(5),
        expireMillis(6),
        ;

        private final int value;

        RuleTag(int value) {
            this.value = value;
        }
    }

    public static void marshal(HotKeyConfig config, Pack pack) {
        Props namespaceProps = new Props();
        namespaceProps.put(NamespaceTag.namespace.value, config.getNamespace());
        ArrayMable<Props> rulesArray = new ArrayMable<>(Props.class);
        List<Rule> rules = config.getRules();
        for (Rule rule : rules) {
            Props props = new Props();
            props.put(RuleTag.name.value, rule.getName());
            props.putInteger(RuleTag.type.value, rule.getType().getValue());
            if (rule.getKeyConfig() != null) {
                props.put(RuleTag.keyConfig.value, rule.getKeyConfig());
            }
            if (rule.getCheckMillis() != null) {
                props.putLong(RuleTag.checkMillis.value, rule.getCheckMillis());
            }
            if (rule.getCheckThreshold() != null) {
                props.putLong(RuleTag.checkThreshold.value, rule.getCheckMillis());
            }
            if (rule.getExpireMillis() != null) {
                props.putLong(RuleTag.expireMillis.value, rule.getExpireMillis());
            }
            rulesArray.add(props);
        }
        pack.putMarshallable(namespaceProps);
        pack.putMarshallable(rulesArray);
    }

    public static HotKeyConfig unmarshal(Unpack unpack) {
        Props namespaceProps = new Props();
        ArrayMable<Props> rulesArray = new ArrayMable<>(Props.class);
        unpack.popMarshallable(namespaceProps);
        unpack.popMarshallable(rulesArray);
        HotKeyConfig config = new HotKeyConfig();
        config.setNamespace(namespaceProps.get(NamespaceTag.namespace.value));
        List<Rule> rules = new ArrayList<>();
        for (Props props : rulesArray.list) {
            Rule rule = new Rule();
            rule.setName(props.get(RuleTag.name.value));
            rule.setType(RuleType.getByValue(props.getInteger(RuleTag.type.value)));
            if (props.containsKey(RuleTag.keyConfig.value)) {
                rule.setKeyConfig(props.get(RuleTag.keyConfig.value));
            }
            if (props.containsKey(RuleTag.checkMillis.value)) {
                rule.setCheckMillis(props.getLong(RuleTag.checkMillis.value));
            }
            if (props.containsKey(RuleTag.checkThreshold.value)) {
                rule.setCheckThreshold(props.getLong(RuleTag.checkThreshold.value));
            }
            if (props.containsKey(RuleTag.expireMillis.value)) {
                rule.setExpireMillis(props.getLong(RuleTag.expireMillis.value));
            }
            rules.add(rule);
        }
        config.setRules(rules);
        return config;
    }
}
