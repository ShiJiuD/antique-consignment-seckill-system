package com.antique.service;

import com.antique.entity.Message;
import com.antique.vo.MessageVO;
import com.antique.vo.PageResultVO;
import com.antique.vo.UnreadCountVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 消息服务接口 — Controller 只依赖这个接口，实现细节在 impl 里
 *
 * <p>继承 IService<Message> 自动获得 save/page/update 等方法
 * （实现类再继承 ServiceImpl 就齐活了，这是 MyBatis-Plus 的标准组合）。
 */
public interface MessageService extends IService<Message> {

    /**
     * 下单成功发送订单消息（写库 + WebSocket 推送）
     * 由 OrderServiceImpl.createOrder 的步骤 9 调用
     */
    void sendOrderMessage(Long userId, String orderNo);

    /** 分页查询当前用户消息（创建时间倒序） */
    PageResultVO<MessageVO> pageList(Long userId, Integer page, Integer size);

    /** 未读消息数（前端角标） */
    UnreadCountVO unreadCount(Long userId);

    /** 标记已读（仅本人消息，幂等） */
    void markRead(Long userId, Long messageId);
}
