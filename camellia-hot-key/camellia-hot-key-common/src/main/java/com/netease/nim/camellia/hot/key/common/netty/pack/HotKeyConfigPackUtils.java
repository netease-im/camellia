package com.netease.nim.camellia.hot.key.common.netty.pack;

import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.common.model.NamespaceType;
import com.netease.nim.camellia.hot.key.common.model.Rule;
import com.netease.nim.camellia.hot.key.common.model.RuleType;
import com.netease.nim.camellia.hot.key.common.netty.codec.ArrayMable;
import com.netease.nim.camellia.hot.key.common.netty.codec.Pack;
import com.netease.nim.camellia.hot.key.common.netty.codec.Property;
import com.netease.nim.camellia.hot.key.common.netty.codec.Unpack;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/5/8
 */
public class HotKeyConfigPackUtils {

    private enum NamespaceTag {
        namespace(1),
        type(2),
        ;

        private final int value;

        NamespaceTag(int value) {
            this.value = value;
        }
    }

    private enum RuleTag {
        name(1),
        type(2),
        checkMillis(3),
        checkThreshold(4),
        expireMillis(5),
        ;

        private final int value;

        RuleTag(int value) {
            this.value = value;
        }
    }

    public static void marshal(HotKeyConfig config, Pack pack) {
        Property namespaceProps = new Property();
        namespaceProps.put(HotKeyConfigPackUtils.NamespaceTag.namespace.value, config.getNamespace());
        namespaceProps.putInteger(HotKeyConfigPackUtils.NamespaceTag.type.value, config.getType().getValue());
        ArrayMable<Property> rulesArray = new ArrayMable<>(Property.class);
        List<Rule> rules = config.getRules();
        for (Rule rule : rules) {
            Property property = new Property();
            property.put(HotKeyConfigPackUtils.RuleTag.name.value, rule.getName());
            property.putInteger(HotKeyConfigPackUtils.RuleTag.type.value, rule.getType().getValue());
            if (rule.getCheckMillis() != null) {
                property.putLong(HotKeyConfigPackUtils.RuleTag.checkMillis.value, rule.getCheckMillis());
            }
            if (rule.getCheckThreshold() != null) {
                property.putLong(HotKeyConfigPackUtils.RuleTag.checkThreshold.value, rule.getCheckMillis());
            }
            if (rule.getExpireMillis() != null) {
                property.putLong(HotKeyConfigPackUtils.RuleTag.expireMillis.value, rule.getExpireMillis());
            }
            rulesArray.add(property);
        }
        pack.putMarshallable(namespaceProps);
        pack.putMarshallable(rulesArray);
    }

    public static HotKeyConfig unmarshal(Unpack unpack) {
        Property namespaceProps = new Property();
        ArrayMable<Property> rulesArray = new ArrayMable<>(Property.class);
        unpack.popMarshallable(namespaceProps);
        unpack.popMarshallable(rulesArray);
        HotKeyConfig config = new HotKeyConfig();
        config.setNamespace(namespaceProps.get(HotKeyConfigPackUtils.NamespaceTag.namespace.value));
        config.setType(NamespaceType.getByValue(namespaceProps.getInteger(HotKeyConfigPackUtils.NamespaceTag.type.value)));
        List<Rule> rules = new ArrayList<>();
        for (Property property : rulesArray.list) {
            Rule rule = new Rule();
            rule.setName(property.get(HotKeyConfigPackUtils.RuleTag.name.value));
            rule.setType(RuleType.getByValue(property.getInteger(HotKeyConfigPackUtils.RuleTag.type.value)));
            if (property.containsKey(HotKeyConfigPackUtils.RuleTag.checkMillis.value)) {
                rule.setCheckMillis(property.getLong(HotKeyConfigPackUtils.RuleTag.checkMillis.value));
            }
            if (property.containsKey(HotKeyConfigPackUtils.RuleTag.checkThreshold.value)) {
                rule.setCheckThreshold(property.getLong(HotKeyConfigPackUtils.RuleTag.checkThreshold.value));
            }
            if (property.containsKey(HotKeyConfigPackUtils.RuleTag.expireMillis.value)) {
                rule.setExpireMillis(property.getLong(HotKeyConfigPackUtils.RuleTag.expireMillis.value));
            }
            rules.add(rule);
        }
        config.setRules(rules);
        return config;
    }
}
