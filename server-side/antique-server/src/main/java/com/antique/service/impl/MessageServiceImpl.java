package com.antique.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.antique.constant.MessageConstant;
import com.antique.entity.Message;
import com.antique.exception.AuthException;
import com.antique.mapper.MessageMapper;
import com.antique.service.MessageService;
import com.antique.util.TimeUtil;
import com.antique.vo.MessageVO;
import com.antique.vo.PageResultVO;
import com.antique.vo.UnreadCountVO;
import com.antique.websocket.WebSocketSessionManager;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息服务实现 — 下单消息发送（写库 + 推送）与查询/已读
 *
 * <h3>下单消息双写（本模块核心逻辑）</h3>
 * <ol>
 *   <li><b>写库</b>：INSERT message（与下单同事务，下单失败消息随之回滚）——保证消息不丢</li>
 *   <li><b>推送</b>：WebSocketSessionManager.sendToUser 实时喊一声——保证在线用户秒看到</li>
 * </ol>
 * <p>推送失败/用户离线只记日志，靠落库兜底，所以推送永远不能影响下单主流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final MessageMapper messageMapper;
    private final WebSocketSessionManager sessionManager;  // "在线登记簿"：推送就靠它

    /** 消息类型：订单消息（对应 message.type） */
    private static final int TYPE_ORDER = 2;

    // ==================== 发送 ====================

    @Override
    public void sendOrderMessage(Long userId, String orderNo) {
        // ----- ① 写库：组装一条订单消息插入 message 表 -----
        Message message = new Message()
                .setUserId(userId)
                .setType(TYPE_ORDER)
                .setTitle(MessageConstant.MESSAGE_TITLE_ORDER)
                .setContent(String.format(MessageConstant.MESSAGE_ORDER_CREATED, orderNo))  // 内容模板："您的订单 %s 已创建成功..."
                .setIsRead(0);  // 初始未读，前端角标按 is_read=0 统计
        save(message);  // INSERT；createTime 自动填充；插入后 message.getId() 有值

        // ----- ② 推送：给该用户所有在线连接发 JSON（没人在线则静默跳过） -----
        sessionManager.sendToUser(userId, buildNewMessagePayload(message));
    }

    /**
     * 组装推送 JSON — 协议见《05消息接口文档.md》：
     * {"type":"newMessage","data":{id,messageType,title,content,createdTime}}
     */
    private String buildNewMessagePayload(Message message) {
        JSONObject data = JSONUtil.createObj()
                .set("id", message.getId())
                .set("messageType", message.getType())
                .set("title", message.getTitle())
                .set("content", message.getContent())
                .set("createdTime", TimeUtil.format(message.getCreateTime()));
        return JSONUtil.createObj()
                .set("type", "newMessage")
                .set("data", data)
                .toString();
    }

    // ==================== 查询 ====================

    @Override
    public PageResultVO<MessageVO> pageList(Long userId, Integer page, Integer size) {
        // 参数兜底：页码/条数没传或非法时用默认值
        int pageNum = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;

        // 分页查询：只查自己的消息（eq user_id），最新的在前
        IPage<Message> result = page(new Page<>(pageNum, pageSize),
                Wrappers.<Message>lambdaQuery()
                        .eq(Message::getUserId, userId)
                        .orderByDesc(Message::getCreateTime));

        // 实体转 VO：只把前端需要的字段挑出来
        List<MessageVO> list = result.getRecords().stream()
                .map(this::toVO)
                .toList();
        return PageResultVO.<MessageVO>builder()
                .list(list)
                .total(result.getTotal())
                .page(pageNum)
                .size(pageSize)
                .build();
    }

    @Override
    public UnreadCountVO unreadCount(Long userId) {
        long count = messageMapper.countUnread(userId);
        return UnreadCountVO.builder().count((int) count).build();
    }

    // ==================== 已读 ====================

    @Override
    public void markRead(Long userId, Long messageId) {
        // UPDATE ... WHERE id=? AND user_id=?
        // 不带 is_read=0 条件：已读过的消息重复调用也成功（幂等无副作用，与接口文档一致）；
        // 带 user_id 条件：只能操作自己的消息，防越权
        boolean updated = update(Wrappers.<Message>lambdaUpdate()
                .set(Message::getIsRead, 1)
                .set(Message::getReadTime, LocalDateTime.now())
                .eq(Message::getId, messageId)
                .eq(Message::getUserId, userId));

        // 影响行数 0 = 消息不存在/不是自己的 → 抛业务异常，全局处理器转成 {code:0,msg:"消息不存在"}
        if (!updated) {
            throw new AuthException(MessageConstant.MESSAGE_NOT_EXIST);
        }
    }

    // ==================== 组装 ====================

    private MessageVO toVO(Message message) {
        return MessageVO.builder()
                .id(message.getId())
                .type(message.getType())
                .title(message.getTitle())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .createdTime(TimeUtil.format(message.getCreateTime()))
                .build();
    }
}
