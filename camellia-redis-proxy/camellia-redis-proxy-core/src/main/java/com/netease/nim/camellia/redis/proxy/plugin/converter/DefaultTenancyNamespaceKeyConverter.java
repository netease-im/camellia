package com.netease.nim.camellia.redis.proxy.plugin.converter;

import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.util.Utils;


/**
 * Tenancy Namespace Key Converter make sure that
 * key single key and key contains hashtag are in the same slot
 *
 * <h3>Normal cases </h3>
 *
 * <ul>
 *   <li>original key: key -> 1|default|key
 *   <li>original key: {key} -> 1|default|{1|default|key}
 *   <li>original key: key:{key} -> 1|default|key:{1|default|key}
 * </ul>
 *
 * <h3>Special cases </h3>
 *
 * <i>the original key contains an odd number of curly braces or not a pair of curly braces</i>
 *
 * <ul>
 *   <li>single curly brace: {key -> 1|default|{key
 *   <li>single curly brace: key} -> 1|default|key}
 *   <li>not a pair of curly braces: prefix}:{key} -> 1|default|prefix}:{1|default|key}
 *   <li>multiple hashtag key:{key1}{key2} -> 1|default|key:{1|default|key1}{key2}
 * </ul>
 *
 * <h3>Test case: </h3>
 *
 * <ul>
 *   <li>mset k1 v1 {k1} {k1} {k1}:key {k1}:key key:{k1} key:{k1} {k1}:key:{k1} {k1}:key:{k1}
 *   <li>mget k1 {k1} {k1}:key key:{k1} {k1}:key:{k1}
 * </ul>
 *
 * Created by caojiajun on 2022/12/7
 */
public class DefaultTenancyNamespaceKeyConverter implements KeyConverter {

    private static final String SEPARATOR_STRING = "|";
    private static final byte[] OPEN_CURLY_BRACE = Utils.stringToBytes("{");
    private static final byte[] CLOSE_CURLY_BRACE = Utils.stringToBytes("}");

    @Override
    public byte[] convert(
            CommandContext commandContext, RedisCommand redisCommand, byte[] originalKey) {
        if (commandContext.getBid() == null
                || commandContext.getBgroup() == null
                || commandContext.getBid() <= 0) {
            return originalKey;
        }
        byte[] prefix = buildPrefix(commandContext);

        // Add a prefix to the original key
        byte[] convertedKey = new byte[originalKey.length + prefix.length];
        System.arraycopy(prefix, 0, convertedKey, 0, prefix.length);
        System.arraycopy(originalKey, 0, convertedKey, prefix.length, originalKey.length);

        // Check the original key contains a hashtag -> add a prefix to the hashtag
        Hashtag hashtag = findFirstHashtag(convertedKey);
        if (hashtag == null) {
            return convertedKey;
        }
        // Replace the old hashtag with the new hashtag with prefix
        // Create new hashtag
        byte[] newHashtag = new byte[hashtag.length + prefix.length];
        System.arraycopy(prefix, 0, newHashtag, 0, prefix.length);
        System.arraycopy(hashtag.bytes, 0, newHashtag, prefix.length, hashtag.length);
        // Replace the old hashtag with the new hashtag
        byte[] newConvertedKey = new byte[convertedKey.length + prefix.length];
        System.arraycopy(convertedKey, 0, newConvertedKey, 0, hashtag.start);
        System.arraycopy(newHashtag, 0, newConvertedKey, hashtag.start, newHashtag.length);
        System.arraycopy(
                convertedKey,
                hashtag.end,
                newConvertedKey,
                hashtag.start + newHashtag.length,
                convertedKey.length - hashtag.end);
        return newConvertedKey;
    }

    @Override
    public byte[] reverseConvert(
            CommandContext commandContext, RedisCommand redisCommand, byte[] convertedKey) {
        if (commandContext.getBid() == null
                || commandContext.getBgroup() == null
                || commandContext.getBid() <= 0) {
            return convertedKey;
        }
        byte[] prefix = buildPrefix(commandContext);
        if (convertedKey.length < prefix.length) {
            return convertedKey;
        }
        boolean prefixMatch = isPrefixMatch(convertedKey, prefix);
        if (!prefixMatch) {
            return convertedKey;
        }
        byte[] originalKey = new byte[convertedKey.length - prefix.length];
        System.arraycopy(convertedKey, prefix.length, originalKey, 0, originalKey.length);

        // Check the original key contains a hashtag -> remove a prefix to the hashtag
        Hashtag hashtag = findFirstHashtag(originalKey);
        if (hashtag == null) {
            return originalKey;
        }
        if (!isPrefixMatch(hashtag.getBytes(), prefix)) {
            return originalKey;
        }
        // Replace the old hashtag with the new hashtag without prefix
        // Create new hashtag
        byte[] newHashtag = new byte[hashtag.length - prefix.length];
        System.arraycopy(hashtag.getBytes(), prefix.length, newHashtag, 0, newHashtag.length);
        // Replace the old hashtag with the new hashtag
        byte[] newOriginalKey = new byte[originalKey.length - prefix.length];
        System.arraycopy(originalKey, 0, newOriginalKey, 0, hashtag.start);
        System.arraycopy(newHashtag, 0, newOriginalKey, hashtag.start, newHashtag.length);
        System.arraycopy(
                originalKey,
                hashtag.end,
                newOriginalKey,
                hashtag.start + newHashtag.length,
                originalKey.length - hashtag.end);
        return newOriginalKey;
    }

    /**
     * Build prefix from bid, bgroup
     *
     * @param commandContext command context
     * @return prefix
     */
    private byte[] buildPrefix(CommandContext commandContext) {
        return Utils.stringToBytes(
                commandContext.getBid() + SEPARATOR_STRING + commandContext.getBgroup() + SEPARATOR_STRING);
    }

    /**
     * Check if key matches the prefix
     *
     * @param key    key
     * @param prefix prefix
     * @return true if key matches the prefix, otherwise false
     */
    private boolean isPrefixMatch(byte[] key, byte[] prefix) {
        boolean prefixMatch = true;
        for (int i = 0; i < prefix.length; i++) {
            if (prefix[i] != key[i]) {
                prefixMatch = false;
                break;
            }
        }
        return prefixMatch;
    }

    /**
     * Find the first hashtag of key
     *
     * @param key key
     * @return hashtag if found, otherwise return null
     */
    private Hashtag findFirstHashtag(byte[] key) {
        int openCurlyBraceIndex = -1;
        if (key.length <= 2) {
            return null;
        }
        for (int i = 0; i < key.length; i++) {
            if (key[i] == OPEN_CURLY_BRACE[0]) {
                openCurlyBraceIndex = i;
            } else if (key[i] == CLOSE_CURLY_BRACE[0]) {
                if (openCurlyBraceIndex >= 0 && i - openCurlyBraceIndex > 1) {
                    // found a pair of curly braces -> hashtag
                    // and make sure that the hashtag is not empty {}
                    int start = openCurlyBraceIndex + 1;
                    int end = i - 1;
                    byte[] hashtagValue = new byte[end - start + 1];
                    System.arraycopy(key, start, hashtagValue, 0, hashtagValue.length);
                    return new Hashtag(start, hashtagValue);
                }
            }
        }
        return null;
    }

    private static class Hashtag {
        private final int start;
        private final int end;
        private final int length;
        private final byte[] bytes;

        private Hashtag(int start, byte[] bytes) {
            this.start = start;
            this.bytes = bytes;
            this.length = bytes.length;
            this.end = start + length;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public int getLength() {
            return length;
        }

        public byte[] getBytes() {
            return bytes;
        }
    }
}
