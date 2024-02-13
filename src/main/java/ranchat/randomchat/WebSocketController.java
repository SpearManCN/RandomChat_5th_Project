package ranchat.randomchat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.repository.query.Param;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class WebSocketController {
    @Autowired
    private SimpMessagingTemplate messageTemplate;

    int totUserNo = 0;
    int brokerNo = 0;
    Map<String, Set<String>> brokers = new ConcurrentHashMap<String, Set<String>>();
    @MessageMapping("/chat/sendMessage/{brokerName}") // 클라이언트가 chat.sendMessage 엔드포인트로 메세지 전송시 /topic/public 으로전송
    @SendTo("/topic/public/{brokerName}")
    public ChatMessage sendMessage(@DestinationVariable(value="brokerName") String brokerName, @Payload ChatMessage chatMessage) {
        System.out.println("sendMessage발생!!");
        if(chatMessage.getType()==MessageType.JOIN){
            System.out.println("JOIN이 들어왔음!!");
            BrokerDTO brokerDTO = findRoom(chatMessage.getSender());
            System.out.println(brokerDTO);
            if(brokerDTO.getMemberName()==null){ //방을 만든 사람이라면
                chatMessage.setBrokerName(brokerDTO.getBrokerName()); // 방이름을 넣어줌
            }else{  //이미 있는 방에 들어간 사람이라면
                chatMessage.setBrokerName(brokerDTO.getBrokerName()); // 방이름과
                chatMessage.setReceiver(brokerDTO.getMemberName());   // 방에 원래있는사람의 이름을 넣어줌
                messageTemplate.convertAndSend("/topic/public/"+brokerDTO.getMemberName(), chatMessage);
            }
        }
        return chatMessage;
    }

    @MessageMapping("/chat/addUser/{roomId}")
    @SendTo("/topic/public/{roomId}")
    public ChatMessage addUser(ChatMessage chatMessage) {
        return chatMessage;
    }

    @MessageMapping("/chat/refresh")
    @SendTo("/totUser/public")
    public ChatMessage refresh(ChatMessage chatMessage) {
        System.out.println("refresh 까지느됨" + chatMessage.getTotUser());
        return chatMessage;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        totUserNo++;
        ChatMessage tmp = new ChatMessage();
        tmp.setTotUser(totUserNo);
        System.out.println("connect 이벤트 발생!!");
//        messageTemplate.convertAndSend("/app/chat.refresh", tmp);
//        MessageHeaders headers = event.getMessage().getHeaders();
//        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());

    }
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        totUserNo--;
//        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
//        String sessionId = headerAccessor.getSessionId();
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        // WebSocket 세션에서 "id" 변수를 꺼내와서 사용
        String sessionId = (String) accessor.getSessionAttributes().get("sessionId");

        // 세션아이디로 Map에서 현재방 찾아서 없애야함 그리고 상대방도 disconnect시켜야함.
        System.out.println("연결종료 발동");
        removeSession(sessionId);

//        ChatMessage tmp = new ChatMessage();
//
//        tmp.setTotUser(totUserNo);
//        tmp.setType(MessageType.LEAVE);
//        messageTemplate.convertAndSend("/topic/public/"+sessionId, tmp);

    }



    public void removeSession(String sessionId){
        System.out.println("removeSession 발동");
        String key = null;
        ChatMessage tmp = new ChatMessage();
        tmp.setType(MessageType.LEAVE);
        System.out.println("Map 은");
        System.out.println(brokers);
        System.out.println("였는데, ");
        for(Map.Entry<String, Set<String>> entry : brokers.entrySet()){
            key = entry.getKey();
            System.out.println("엔트리.getkey는 : " + key);
            Set<String> values = entry.getValue();
            System.out.println("엔트리.getvalue는 : " + values);
            System.out.println("contains 인지 참 거짓 : " + values.contains(sessionId));
            System.out.println("sessionId : " + sessionId);
            if(values.contains(sessionId)){
                System.out.println("values.contains 발동");
                for (String value : values) {
                    messageTemplate.convertAndSend("/topic/public/"+value, tmp);
                }
                brokers.remove(key);
                System.out.println("brokers.remove 후 brokers : "+brokers);
                return;
            }
        }
    }

//    @RequestMapping("/findRoomProc")
//    @ResponseBody
//    public String findFitRoom(HttpServletRequest request){
//        String sessionId =  request.getSession().getId();
//        String key = "";
//
//        for(Map.Entry<String, Set<String>> entry : brokers.entrySet()){
//            Set<String> values = entry.getValue();
//            if(values.size() <= 1){
//                key = entry.getKey();
//                values.add(sessionId);
//                break;
//            }
//        }
//        if(key.equals("")){
//            brokers.put("room"+brokerNo , new HashSet<>());
//            brokerNo++;
//            key = sessionId;
//        }
//        System.out.println("키값은"+key);
//        System.out.println("세션아이디는"+sessionId);
//        return key;
//
//    }

    public BrokerDTO findRoom(String sessionId){
        BrokerDTO brokerDTO = new BrokerDTO();
        brokerDTO.setBrokerName("none");
        System.out.println("findRoom은 실행됨");
        for(Map.Entry<String, Set<String>> entry : brokers.entrySet()){
            Set<String> values = entry.getValue();
            System.out.println("엔트리의 밸류 셋값"+values.size());
            if(values.size() == 1){
                System.out.println("방이있을때 생성되는코드 발동");
                for (String value : values) {
                    brokerDTO.setMemberName(value);
                    brokerDTO.setBrokerName(entry.getKey());
                }
                values.add(sessionId);
                break;
            }
        }
        if(brokerDTO.getBrokerName().equals("none")){
            System.out.println("방이없을때 생성되는코드 발동");
            Set<String> hashSet = new HashSet<>();
            hashSet.add(sessionId);
            brokers.put("room"+brokerNo , hashSet);
            brokerDTO.setBrokerName("room"+brokerNo);
            System.out.println(brokers);
            brokerNo++;
        }
        return brokerDTO;

    }





    @RequestMapping("/home")
    public String home(Model model, HttpServletRequest request){
        HttpSession session = request.getSession();

        model.addAttribute("sessionId",session.getId());
//        System.out.println(session);
        return "index";

    }

    @RequestMapping("/updateUserNo")
    @ResponseBody
    public int updateUserNo(){
        return totUserNo;
    }

}

