package com.zhuangxv.bot.handler.message;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zhuangxv.bot.annotation.GroupMessageHandler;
import com.zhuangxv.bot.core.Bot;
import com.zhuangxv.bot.core.BotFactory;
import com.zhuangxv.bot.event.message.GroupMessageEvent;
import com.zhuangxv.bot.handler.EventHandler;
import com.zhuangxv.bot.message.CacheMessage;
import com.zhuangxv.bot.message.Message;
import com.zhuangxv.bot.message.MessageChain;
import com.zhuangxv.bot.message.MessageTypeHandle;
import com.zhuangxv.bot.message.support.AtMessage;
import com.zhuangxv.bot.util.ArrayUtils;
import com.zhuangxv.bot.utilEnum.IgnoreItselfEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GroupMessageEventHandler implements EventHandler {

    @Override
    public void handle(JSONObject jsonObject, Bot bot) {
        if (!GroupMessageEvent.isSupport(jsonObject)) {
            return;
        }
        GroupMessageEvent groupMessageEvent = jsonObject.toJavaObject(GroupMessageEvent.class);
        MessageChain messageChain = new MessageChain();
        for (int i = 0; i < groupMessageEvent.getMessage().size(); i++) {
            messageChain.add(MessageTypeHandle.getMessage(groupMessageEvent.getMessage().getJSONObject(i)));
        }
        log.debug(messageChain.toMessageString());
        CacheMessage cacheMessage = new CacheMessage();
        cacheMessage.setSenderId(groupMessageEvent.getUserId());
        cacheMessage.setMessageChain(messageChain);
        bot.pushGroupCacheMessageChain(groupMessageEvent.getGroupId(), groupMessageEvent.getMessageId(), cacheMessage);
        List<Object> resultList = BotFactory.handleMethod(bot, groupMessageEvent, handlerMethod -> {
            if (!handlerMethod.getMethod().isAnnotationPresent(GroupMessageHandler.class)) {
                return false;
            }
            GroupMessageHandler groupMessageHandler = handlerMethod.getMethod().getAnnotation(GroupMessageHandler.class);
            if (groupMessageHandler.ignoreItself().equals(IgnoreItselfEnum.IGNORE_ITSELF) && "message_sent".equals(groupMessageEvent.getEventType())) {
                return false;
            } else if (groupMessageHandler.ignoreItself().equals(IgnoreItselfEnum.ONLY_ITSELF) && !"message_sent".equals(groupMessageEvent.getEventType())){
                return false;
            }
            if (groupMessageHandler.groupIds().length > 0 && !ArrayUtils.contain(groupMessageHandler.groupIds(), groupMessageEvent.getGroupId())) {
                return false;
            }
            if (groupMessageHandler.isAt()){
                JSONArray jsonArray = groupMessageEvent.getMessage();
                List<String> typeArr = jsonArray.stream().map(o -> (String)((JSONObject) o).get("type")).collect(Collectors.toList());
                if (!typeArr.contains("at")){
                    return false;
                }
            }
            if (ArrayUtils.contain(groupMessageHandler.excludeGroupIds(), groupMessageEvent.getGroupId())) {
                return false;
            }
            if (groupMessageHandler.senderIds().length > 0 && !ArrayUtils.contain(groupMessageHandler.senderIds(), groupMessageEvent.getUserId())) {
                return false;
            }
            if (ArrayUtils.contain(groupMessageHandler.excludeSenderIds(), groupMessageEvent.getUserId())) {
                return false;
            }
            if (groupMessageHandler.isAt()) {
                List<Long> atList = new ArrayList<>();
                messageChain.forEach(message -> {
                    if (message instanceof AtMessage) {
                        atList.add(Long.valueOf(((AtMessage) message).getQq()));
                    }
                });
                if (atList.isEmpty() || !atList.contains(groupMessageEvent.getSelfId())) {
                    return false;
                }
            }
            return "none".equals(groupMessageHandler.regex()) || messageChain.toString().matches(groupMessageHandler.regex());
        }, "message");
        for (Object result : resultList) {
            try {
                if (result instanceof Message) {
                    bot.getGroup(groupMessageEvent.getGroupId()).sendMessage((Message) result);
                }
                if (result instanceof MessageChain) {
                    bot.getGroup(groupMessageEvent.getGroupId()).sendMessage((MessageChain) result);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

}
