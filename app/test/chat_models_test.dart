import 'package:chess_card_app/chat_models.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('parses chat message', () {
    final message = ChatMessageModel.fromJson({
      'messageId': 'message-1',
      'roomId': 'room-1',
      'senderPlayerId': 'player-1',
      'content': 'hello',
      'sentAt': '2026-07-10T08:00:00Z',
    });

    expect(message.messageId, 'message-1');
    expect(message.roomId, 'room-1');
    expect(message.senderPlayerId, 'player-1');
    expect(message.content, 'hello');
    expect(message.sentAt, DateTime.parse('2026-07-10T08:00:00Z'));
  });
}
