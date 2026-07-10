class ChatMessageModel {
  const ChatMessageModel({
    required this.messageId,
    required this.roomId,
    required this.senderPlayerId,
    required this.content,
    required this.sentAt,
  });

  final String messageId;
  final String roomId;
  final String senderPlayerId;
  final String content;
  final DateTime sentAt;

  factory ChatMessageModel.fromJson(Map<String, dynamic> json) {
    return ChatMessageModel(
      messageId: json['messageId'] as String,
      roomId: json['roomId'] as String,
      senderPlayerId: json['senderPlayerId'] as String,
      content: json['content'] as String,
      sentAt: DateTime.parse(json['sentAt'] as String),
    );
  }
}
