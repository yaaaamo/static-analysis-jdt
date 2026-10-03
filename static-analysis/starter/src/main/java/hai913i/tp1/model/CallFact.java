package hai913i.tp1.model;

public record CallFact(String callerId,           // method that contains the call
                       String name,               // name of the called method
                       String targetId,           // resolved target, or null
                       String staticReceiverType, // static type of the receiver
                       int line,
                       boolean resolved,
                       boolean internal) {}       // target declared in the analysed project
