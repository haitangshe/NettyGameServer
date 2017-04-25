package com.wolf.shoot.service.net.pipeline;

import com.wolf.shoot.common.config.GameServerConfig;
import com.wolf.shoot.common.config.GameServerConfigService;
import com.wolf.shoot.common.constant.Loggers;
import com.wolf.shoot.manager.LocalMananger;
import com.wolf.shoot.service.lookup.NetTcpSessionLoopUpService;
import com.wolf.shoot.service.net.MessageAttributeEnum;
import com.wolf.shoot.service.net.message.AbstractNetMessage;
import com.wolf.shoot.service.net.message.AbstractNetProtoBufMessage;
import com.wolf.shoot.service.net.message.command.MessageCommand;
import com.wolf.shoot.service.net.message.command.MessageCommandEnum;
import com.wolf.shoot.service.net.message.registry.MessageRegistry;
import com.wolf.shoot.service.net.process.GameTcpMessageProcessor;
import com.wolf.shoot.service.net.session.NettyTcpSession;
import com.wolf.shoot.service.net.session.builder.NettyTcpSessionBuilder;
import com.wolf.shoot.service.rpc.server.RpcConfig;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

/**
 * Created by jiangwenping on 17/2/13.
 * 处理管道
 */
@Service
public class DefaultTcpServerPipeLine implements IServerPipeLine {
    public static Logger logger = Loggers.sessionLogger;

    @Override
    public void dispatchAction(Channel channel, AbstractNetMessage abstractNetMessage) {
        short commandId = abstractNetMessage.getNetMessageHead().getCmd();
        MessageRegistry messageRegistry = LocalMananger.getInstance().getLocalSpringServiceManager().getMessageRegistry();
        MessageCommand messageCommand = messageRegistry.getMessageCommand(commandId);
        if (logger.isDebugEnabled()) {
            logger.debug("RECV_TCP_PROBUF_MESSAGE:" + MessageCommandEnum.values()[commandId]);
        }

        AbstractNetProtoBufMessage abstractNetProtoBufMessage = (AbstractNetProtoBufMessage) abstractNetMessage;
        NetTcpSessionLoopUpService netTcpSessionLoopUpService = LocalMananger.getInstance().getLocalSpringServiceManager().getNetTcpSessionLoopUpService();
        long sessonId = channel.attr(NettyTcpSessionBuilder.channel_sessionId).get();
        NettyTcpSession nettySession = (NettyTcpSession) netTcpSessionLoopUpService.lookup(sessonId);
        if (nettySession == null) {
            logger.error("tcp netsession null channelId is:" + channel.id().asLongText());
        }

//        abstractNetProtoBufMessage.setSessionId(nettySession.getSessionId());

        //检查是否可以处理该消息
        GameServerConfigService gameServerConfigService = LocalMananger.getInstance().getLocalSpringServiceManager().getGameServerConfigService();

        //如果是通用消息，不进行服务器检测

        RpcConfig rpcConfig = gameServerConfigService.getRpcConfig();
        if(!rpcConfig.validServer(messageCommand.bo_id)) {
            if(logger.isDebugEnabled()) {
                logger.debug("discard tcp message  sessionId:" + nettySession.getSessionId() + "playerId:" + nettySession.getPlayerId() + " messageId is " + commandId);
            }
            return;
        }

        GameServerConfig gameServerConfig = gameServerConfigService.getGameServerConfig();
        if (gameServerConfig.isDevelopModel() && logger.isDebugEnabled()) {
            logger.debug("sessionId" + nettySession.getSessionId() + " playerId" + nettySession.getPlayerId() + " read tcp message" + commandId + "info" + abstractNetProtoBufMessage.toAllInfoString());
        }

        if (messageCommand.is_need_filter()) {
            int serial = abstractNetMessage.getSerial();
            long playerId = nettySession.getPlayerId();
//            PlatformType platformType = nettySession.getPlatformType();
//            if(platformType == null){
//                AbstractGameMessage response = GameUtils.errorCallMessage(message.getCommandId(), serial, MessageErrorEnum.COMMON_MESSAGE_PLATFROM_NO_EXIST);
//                if(gameServerConfig.getServerType() == BOConst.BO_GAME){
//                    response = GameUtils.errorCallMessage(message.getCommandId(), serial, MessageErrorEnum.GAME_ERROR_COMMON_MESSAGE_PLATFROM_NO_EXIST);
//                }
//                nettySession.write(response);
//                return;
//
//            }

//            A5Player a5Player = ObjectAccessorEx.getA5Player(playerId, platformType);
//            if(a5Player == null){
//                AbstractGameMessage response = GameUtils.errorCallMessage(message.getCommandId(), serial, MessageErrorEnum.COMMON_MESSAGE_PLAYER_NO_EXIST);
//                if(gameServerConfig.getServerType() == BOConst.BO_GAME){
//                    response = GameUtils.errorCallMessage(message.getCommandId(), serial, MessageErrorEnum.GAME_ERROR_COMMON_MESSAGE_PLAYER_NO_EXIST);
//                }
//                clientSession.write(response);
//                return;
//            }
//
//            if(a5Player.getMsgSerial() == serial){
//                AbstractGameMessage response = GameUtils.errorCallMessage(message.getCommandId(), serial, MessageErrorEnum.COMMON_MESSAGE_ALREADY_HANDLED);
//                clientSession.write(response);
//                return;
//            }
        }

//        //放入处理队列
        abstractNetMessage.setAttribute(MessageAttributeEnum.DISPATCH_SESSION, nettySession);
        GameTcpMessageProcessor gameTcpMessageProcessor = LocalMananger.getInstance().getGameTcpMessageProcessor();
//        if(gameServerConfig.isTcpMessageQueueDirectDispatch()){
//            gameTcpMessageProcessor.directPutTcpMessage(abstractNetMessage);
//        }else{
//           gameTcpMessageProcessor.put(abstractNetProtoBufMessage);
//        }
        //取消判断直接分发
        gameTcpMessageProcessor.directPutTcpMessage(abstractNetMessage);
    }
}
