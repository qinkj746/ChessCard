import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:chess_card_app/api_client.dart';
import 'package:chess_card_app/auth_models.dart';
import 'package:chess_card_app/chat_models.dart';
import 'package:chess_card_app/friend_models.dart';
import 'package:chess_card_app/app_error.dart';
import 'package:chess_card_app/game_page.dart';
import 'package:chess_card_app/main.dart';
import 'package:chess_card_app/models.dart';
import 'package:chess_card_app/record_models.dart';
import 'package:chess_card_app/room_connection.dart';
import 'package:chess_card_app/room_page.dart';
import 'package:chess_card_app/status_banner.dart';

void main() {
  testWidgets('home page exposes single player and room entries',
      (WidgetTester tester) async {
    await tester.pumpWidget(const ChessCardApp());

    expect(find.byIcon(Icons.play_arrow), findsOneWidget);
    expect(find.byIcon(Icons.meeting_room), findsOneWidget);
  });

  testWidgets('clear selection button deselects selected cards',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(home: GamePage(initialGame: playGame)),
    );

    expect(find.byIcon(Icons.clear), findsNothing);

    await tester.tap(find.ancestor(
        of: find.byType(AnimatedContainer).first,
        matching: find.byType(GestureDetector)));
    await tester.pump();

    expect(find.byIcon(Icons.clear), findsOneWidget);

    await tester.tap(find.byIcon(Icons.clear));
    await tester.pump();

    expect(find.byIcon(Icons.clear), findsNothing);
  });

  testWidgets('create game renders declaration options',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      MaterialApp(home: GamePage(api: FakeApiClient(declareGame))),
    );

    await tester.tap(find.byIcon(Icons.play_arrow));
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.flag), findsNWidgets(2));
  });

  testWidgets('ai banker declaration phase hides human declaration actions',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(home: GamePage(initialGame: westBankerDeclareGame)),
    );

    expect(find.byIcon(Icons.flag), findsNothing);
    expect(find.byIcon(Icons.smart_toy), findsOneWidget);
  });

  testWidgets('play phase disables play until a card is selected',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(home: GamePage(initialGame: playGame)),
    );

    FilledButton playButton = tester.widget<FilledButton>(
      find.ancestor(
          of: find.byIcon(Icons.send), matching: find.byType(FilledButton)),
    );
    expect(playButton.onPressed, isNull);

    await tester.tap(find.ancestor(
        of: find.byType(AnimatedContainer).first,
        matching: find.byType(GestureDetector)));
    await tester.pump();

    playButton = tester.widget<FilledButton>(
      find.ancestor(
          of: find.byIcon(Icons.send), matching: find.byType(FilledButton)),
    );
    expect(playButton.onPressed, isNotNull);
  });

  testWidgets('play error keeps selected cards visible',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: GamePage(
          initialGame: playGame,
          api: FakeApiClient(playGame, playError: 'play failed'),
        ),
      ),
    );

    await tester.tap(find.ancestor(
        of: find.byType(AnimatedContainer).first,
        matching: find.byType(GestureDetector)));
    await tester.pump();
    await tester.tap(find.byIcon(Icons.send));
    await tester.pump();

    expect(find.text('play failed'), findsOneWidget);
    expect(find.byIcon(Icons.clear), findsOneWidget);
  });

  testWidgets('finished uncompleted game can start next game',
      (WidgetTester tester) async {
    final api = FakeApiClient(declareGame, nextGame: nextRoundGame);
    await tester.pumpWidget(
      MaterialApp(home: GamePage(initialGame: finishedPendingGame, api: api)),
    );

    await tester.tap(find.byIcon(Icons.skip_next));
    await tester.pumpAndSettle();

    expect(api.nextGameCalls, 1);
    expect(find.byIcon(Icons.smart_toy), findsOneWidget);
  });

  testWidgets('completed game hides next game action',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(home: GamePage(initialGame: finishedGame)),
    );

    expect(find.byIcon(Icons.skip_next), findsNothing);
  });

  testWidgets('kitty phase shows kitty action', (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(home: GamePage(initialGame: kittyGame)),
    );

    expect(find.byIcon(Icons.inventory_2), findsOneWidget);
  });

  testWidgets('current trick displays played card rank',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(home: GamePage(initialGame: trickGame)),
    );

    expect(find.textContaining('K'), findsOneWidget);
  });

  Future<void> pumpRoomPage(WidgetTester tester, {GameApi? api}) async {
    await tester.pumpWidget(
      MaterialApp(home: RoomPage(api: api ?? FakeApiClient(playGame))),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('room page shows create room action after guest init',
      (WidgetTester tester) async {
    await pumpRoomPage(tester);

    expect(find.byIcon(Icons.add), findsOneWidget);
  });

  testWidgets('create room shows owner and start action',
      (WidgetTester tester) async {
    await pumpRoomPage(tester);

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.person), findsAtLeastNWidgets(1));
    expect(find.byIcon(Icons.play_arrow), findsOneWidget);
  });

  testWidgets('room page refreshes when room event arrives',
      (WidgetTester tester) async {
    final events = FakeRoomEventSource();
    final api = FakeApiClient(playGame);
    await tester.pumpWidget(MaterialApp(
      home: RoomPage(
        api: api,
        roomConnectionFactory: (_) => events,
      ),
    ));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();
    events.emit(const RoomEventModel(type: 'ROOM_UPDATED', roomId: 'room-1'));
    await tester.pump();

    expect(api.getRoomCalls, 1);
    expect(events.connectCalls, 1);
  });

  testWidgets('room page ignores stale room events',
      (WidgetTester tester) async {
    final events = FakeRoomEventSource();
    final api = FakeApiClient(playGame);
    await tester.pumpWidget(MaterialApp(
      home: RoomPage(
        api: api,
        roomConnectionFactory: (_) => events,
      ),
    ));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();
    events.emit(const RoomEventModel(
      type: 'ROOM_UPDATED',
      roomId: 'room-1',
      version: 1,
    ));
    await tester.pump();
    events.emit(const RoomEventModel(
      type: 'ROOM_UPDATED',
      roomId: 'room-1',
      version: 2,
    ));
    await tester.pump();

    expect(api.getRoomCalls, 1);
  });
  testWidgets('room page sends chat message and clears input',
      (WidgetTester tester) async {
    final api = FakeApiClient(playGame);
    await pumpRoomPage(tester, api: api);

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), 'hello room');
    await tester.tap(find.byIcon(Icons.send));
    await tester.pumpAndSettle();

    expect(api.sentChatContents, ['hello room']);
    final field = tester.widget<TextField>(find.byType(TextField));
    expect(field.controller?.text, isEmpty);
    expect(find.text('hello room'), findsOneWidget);
  });

  testWidgets('room page appends incoming chat message event',
      (WidgetTester tester) async {
    final events = FakeRoomEventSource();
    final api = FakeApiClient(playGame);
    await tester.pumpWidget(MaterialApp(
      home: RoomPage(
        api: api,
        roomConnectionFactory: (_) => events,
      ),
    ));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();
    events.emit(const RoomEventModel(
      type: 'CHAT_MESSAGE',
      roomId: 'room-1',
      payload: {
        'messageId': 'message-event',
        'roomId': 'room-1',
        'senderPlayerId': 'fake-player',
        'content': 'hello event',
        'sentAt': '2026-07-10T08:00:00Z',
      },
    ));
    await tester.pumpAndSettle();

    expect(find.text('hello event'), findsOneWidget);
  });

  testWidgets('room page invites a listed friend', (WidgetTester tester) async {
    final api = FakeApiClient(playGame);
    api.friendships.add(const FriendshipModel(
      friendshipId: 'friendship-1',
      requesterPlayerId: 'fake-player',
      addresseePlayerId: 'friend-player',
      status: 'ACCEPTED',
    ));
    await pumpRoomPage(tester, api: api);

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();
    await tester.tap(find.byIcon(Icons.person_add));
    await tester.pumpAndSettle();

    expect(api.invitedPlayerIds, ['friend-player']);
  });

  testWidgets('room page responds to incoming room invitation',
      (WidgetTester tester) async {
    final events = FakeRoomEventSource();
    final api = FakeApiClient(playGame);
    await tester.pumpWidget(MaterialApp(
      home: RoomPage(
        api: api,
        roomConnectionFactory: (_) => events,
      ),
    ));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();
    events.emit(const RoomEventModel(
      type: 'ROOM_INVITATION',
      roomId: 'room-1',
      payload: {
        'invitationId': 'invitation-1',
        'roomId': 'room-1',
        'fromPlayerId': 'friend-player',
        'toPlayerId': 'fake-player',
        'status': 'PENDING',
        'createdAt': '2026-07-10T08:00:00Z',
        'expiresAt': '2026-07-10T08:30:00Z',
      },
    ));
    await tester.pumpAndSettle();

    expect(find.textContaining('friend-player'), findsOneWidget);
    await tester.tap(find.byIcon(Icons.check));
    await tester.pumpAndSettle();

    expect(api.respondedInvitations, ['invitation-1:true']);
    expect(find.textContaining('friend-player'), findsNothing);
  });

  testWidgets('start game navigates to game page', (WidgetTester tester) async {
    await pumpRoomPage(tester);

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.play_arrow));
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.send), findsOneWidget);
  });

  testWidgets('game page refreshes when game event arrives',
      (WidgetTester tester) async {
    final events = FakeRoomEventSource();
    final api = FakeApiClient(playGame, refreshedGame: trickGame);
    await tester.pumpWidget(MaterialApp(
      home: GamePage(
        initialGame: playGame,
        api: api,
        playerId: 'fake-player',
        roomId: 'room-1',
        roomEvents: events,
      ),
    ));
    await tester.pump();

    events.emit(const RoomEventModel(
      type: 'GAME_UPDATED',
      roomId: 'room-1',
      gameId: 'game-8',
    ));
    await tester.pump();

    expect(api.getGameCalls, 1);
    expect(api.requestedGameIds, ['game-8']);
    expect(events.connectCalls, 1);
  });

  testWidgets('game page ignores stale game events',
      (WidgetTester tester) async {
    final events = FakeRoomEventSource();
    final api = FakeApiClient(playGame, refreshedGame: trickGame);
    await tester.pumpWidget(MaterialApp(
      home: GamePage(
        initialGame: playGame,
        api: api,
        playerId: 'fake-player',
        roomId: 'room-1',
        roomEvents: events,
      ),
    ));
    await tester.pump();

    events.emit(const RoomEventModel(
      type: 'GAME_UPDATED',
      roomId: 'room-1',
      gameId: 'game-8',
      version: 2,
    ));
    await tester.pump();
    events.emit(const RoomEventModel(
      type: 'GAME_UPDATED',
      roomId: 'room-1',
      gameId: 'game-8',
      version: 1,
    ));
    await tester.pump();

    expect(api.getGameCalls, 1);
  });
  testWidgets('status banner displays error message',
      (WidgetTester tester) async {
    const error = AppError(code: 'TEST', message: 'boom', retryable: false);
    await tester
        .pumpWidget(const MaterialApp(home: StatusBanner(error: error)));

    expect(find.text('boom'), findsOneWidget);
    expect(find.text('retry'), findsNothing);
  });

  testWidgets('status banner retry callback can be tapped',
      (WidgetTester tester) async {
    bool retried = false;
    const error =
        AppError(code: 'NETWORK_ERROR', message: 'offline', retryable: true);
    await tester.pumpWidget(MaterialApp(
      home: StatusBanner(error: error, onRetry: () => retried = true),
    ));

    await tester.tap(find.byType(TextButton));
    expect(retried, isTrue);
  });

  testWidgets('status banner can be dismissed', (WidgetTester tester) async {
    const error =
        AppError(code: 'TEST', message: 'dismissible', retryable: false);
    await tester
        .pumpWidget(const MaterialApp(home: StatusBanner(error: error)));

    expect(find.text('dismissible'), findsOneWidget);
    await tester.tap(find.byIcon(Icons.close));
    await tester.pump();

    expect(find.text('dismissible'), findsNothing);
  });

  testWidgets('action message displays and fades out',
      (WidgetTester tester) async {
    const gameWithMessage = GameStateModel(
      id: 'game-msg',
      phase: 'PLAY',
      levelRank: 'ACE',
      trumpSuit: 'HEART',
      banker: 'SOUTH',
      currentTurn: 'SOUTH',
      attackerScore: 0,
      winningTeam: null,
      levelDelta: 0,
      nextLevelRank: null,
      completed: false,
      handCounts: {'SOUTH': 1, 'WEST': 1, 'NORTH': 1, 'EAST': 1},
      southHand: [CardModel(suit: 'SPADE', rank: 'FIVE', deckIndex: 0)],
      kitty: [],
      currentTrick: {},
      declarationOptions: [],
      lastActionMessage: 'fallback play',
    );

    await tester.pumpWidget(
      const MaterialApp(home: GamePage(initialGame: gameWithMessage)),
    );

    expect(find.text('fallback play'), findsOneWidget);

    await tester.pump(const Duration(seconds: 3));
    await tester.pump();

    final opacity =
        tester.widget<AnimatedOpacity>(find.byType(AnimatedOpacity));
    expect(opacity.opacity, 0);
  });
}

const declareGame = GameStateModel(
  id: 'game-1',
  phase: 'DECLARE',
  levelRank: 'ACE',
  trumpSuit: null,
  banker: null,
  currentTurn: 'SOUTH',
  attackerScore: 0,
  winningTeam: null,
  levelDelta: 0,
  nextLevelRank: null,
  completed: false,
  handCounts: {'SOUTH': 2, 'WEST': 25, 'NORTH': 25, 'EAST': 25},
  southHand: [
    CardModel(suit: null, rank: 'BIG_JOKER', deckIndex: 0),
    CardModel(suit: 'HEART', rank: 'ACE', deckIndex: 0),
  ],
  kitty: [],
  currentTrick: {},
  declarationOptions: ['HEART', 'DIAMOND'],
);

const playGame = GameStateModel(
  id: 'game-2',
  phase: 'PLAY',
  levelRank: 'ACE',
  trumpSuit: 'HEART',
  banker: 'SOUTH',
  currentTurn: 'SOUTH',
  attackerScore: 0,
  winningTeam: null,
  levelDelta: 0,
  nextLevelRank: null,
  completed: false,
  handCounts: {'SOUTH': 1, 'WEST': 1, 'NORTH': 1, 'EAST': 1},
  southHand: [CardModel(suit: 'SPADE', rank: 'FIVE', deckIndex: 0)],
  kitty: [],
  currentTrick: {},
  declarationOptions: [],
);

const finishedGame = GameStateModel(
  id: 'game-3',
  phase: 'FINISHED',
  levelRank: 'ACE',
  trumpSuit: 'HEART',
  banker: 'SOUTH',
  currentTurn: 'WEST',
  attackerScore: 110,
  winningTeam: 'EAST_WEST',
  levelDelta: 1,
  nextLevelRank: 'TWO',
  completed: true,
  handCounts: {'SOUTH': 0, 'WEST': 0, 'NORTH': 0, 'EAST': 0},
  southHand: [],
  kitty: [],
  currentTrick: {},
  declarationOptions: [],
);

const finishedPendingGame = GameStateModel(
  id: 'game-4',
  phase: 'FINISHED',
  levelRank: 'ACE',
  trumpSuit: 'HEART',
  banker: 'SOUTH',
  currentTurn: 'WEST',
  attackerScore: 110,
  winningTeam: 'EAST_WEST',
  levelDelta: 1,
  nextLevelRank: 'TWO',
  completed: false,
  handCounts: {'SOUTH': 0, 'WEST': 0, 'NORTH': 0, 'EAST': 0},
  southHand: [],
  kitty: [],
  currentTrick: {},
  declarationOptions: [],
);

const nextRoundGame = GameStateModel(
  id: 'game-5',
  phase: 'DECLARE',
  levelRank: 'TWO',
  trumpSuit: null,
  banker: 'WEST',
  currentTurn: 'WEST',
  attackerScore: 0,
  winningTeam: null,
  levelDelta: 0,
  nextLevelRank: null,
  completed: false,
  handCounts: {'SOUTH': 25, 'WEST': 25, 'NORTH': 25, 'EAST': 25},
  southHand: [CardModel(suit: null, rank: 'BIG_JOKER', deckIndex: 0)],
  kitty: [],
  currentTrick: {},
  declarationOptions: ['HEART'],
);

const westBankerDeclareGame = GameStateModel(
  id: 'game-6',
  phase: 'DECLARE',
  levelRank: 'TWO',
  trumpSuit: null,
  banker: 'WEST',
  currentTurn: 'WEST',
  attackerScore: 0,
  winningTeam: null,
  levelDelta: 0,
  nextLevelRank: null,
  completed: false,
  handCounts: {'SOUTH': 25, 'WEST': 25, 'NORTH': 25, 'EAST': 25},
  southHand: [
    CardModel(suit: null, rank: 'BIG_JOKER', deckIndex: 0),
    CardModel(suit: 'HEART', rank: 'TWO', deckIndex: 0),
  ],
  kitty: [],
  currentTrick: {},
  declarationOptions: ['HEART'],
);

const kittyGame = GameStateModel(
  id: 'game-7',
  phase: 'KITTY',
  levelRank: 'ACE',
  trumpSuit: 'HEART',
  banker: 'SOUTH',
  currentTurn: 'SOUTH',
  attackerScore: 0,
  winningTeam: null,
  levelDelta: 0,
  nextLevelRank: null,
  completed: false,
  handCounts: {'SOUTH': 33, 'WEST': 25, 'NORTH': 25, 'EAST': 25},
  southHand: [
    CardModel(suit: 'SPADE', rank: 'FIVE', deckIndex: 0),
    CardModel(suit: 'HEART', rank: 'KING', deckIndex: 0),
    CardModel(suit: 'CLUB', rank: 'ACE', deckIndex: 0),
  ],
  kitty: [],
  currentTrick: {},
  declarationOptions: [],
);

const trickGame = GameStateModel(
  id: 'game-8',
  phase: 'PLAY',
  levelRank: 'ACE',
  trumpSuit: 'HEART',
  banker: 'SOUTH',
  currentTurn: 'WEST',
  attackerScore: 0,
  winningTeam: null,
  levelDelta: 0,
  nextLevelRank: null,
  completed: false,
  handCounts: {'SOUTH': 24, 'WEST': 25, 'NORTH': 25, 'EAST': 25},
  southHand: [CardModel(suit: 'SPADE', rank: 'FIVE', deckIndex: 0)],
  kitty: [],
  currentTrick: {
    'SOUTH': [CardModel(suit: 'SPADE', rank: 'KING', deckIndex: 0)],
  },
  declarationOptions: [],
);

class FakeRoomEventSource implements RoomEventSource {
  final StreamController<RoomEventModel> controller =
      StreamController<RoomEventModel>.broadcast();
  int connectCalls = 0;
  int disconnectCalls = 0;
  int reconnectCalls = 0;

  @override
  Stream<RoomEventModel> get events => controller.stream;

  @override
  void connect() {
    connectCalls += 1;
  }

  @override
  void reconnect() {
    reconnectCalls += 1;
  }

  @override
  void disconnect() {
    disconnectCalls += 1;
  }

  void emit(RoomEventModel event) {
    controller.add(event);
  }
}

class FakeApiClient implements GameApi {
  FakeApiClient(
    this.createdGame, {
    GameStateModel? nextGame,
    GameStateModel? refreshedGame,
    this.playError,
  })  : nextGameResult = nextGame ?? createdGame,
        refreshedGameResult = refreshedGame ?? createdGame;

  final GameStateModel createdGame;
  final GameStateModel nextGameResult;
  final GameStateModel refreshedGameResult;
  final String? playError;
  final List<ChatMessageModel> chatMessages = [];
  final List<String> sentChatContents = [];
  final List<FriendshipModel> friendships = [];
  final List<RoomInvitationModel> pendingInvitations = [];
  final List<String> invitedPlayerIds = [];
  final List<String> respondedInvitations = [];
  int nextGameCalls = 0;
  int getRoomCalls = 0;
  int getGameCalls = 0;
  final List<String> requestedGameIds = [];

  @override
  Future<GameStateModel> createGame() async => createdGame;

  @override
  Future<GameStateModel> getGame(String gameId) async {
    getGameCalls += 1;
    requestedGameIds.add(gameId);
    return refreshedGameResult;
  }

  @override
  Future<GameStateModel> declare(String gameId, String suit,
          {String? playerId}) async =>
      createdGame;

  @override
  Future<GameStateModel> kitty(String gameId, List<CardModel> cards,
          {String? playerId}) async =>
      createdGame;

  @override
  Future<GameStateModel> play(String gameId, List<CardModel> cards,
      {String? playerId}) async {
    if (playError != null) {
      throw Exception(playError);
    }
    return createdGame;
  }

  @override
  Future<GameStateModel> aiStep(String gameId) async => createdGame;

  @override
  Future<GameStateModel> nextGame(String gameId) async {
    nextGameCalls += 1;
    return nextGameResult;
  }

  @override
  Future<PlayerProfileModel> createGuestPlayer() async =>
      const PlayerProfileModel(
        playerId: 'fake-player',
        displayName: 'Guest-0001',
        guest: true,
        sessionToken: 'fake-token',
      );

  @override
  Future<AuthSessionModel> register({
    required String username,
    required String password,
    String? playerId,
  }) async =>
      AuthSessionModel(
        playerId: playerId ?? 'fake-player',
        username: username,
        displayName: username,
        sessionToken: 'fake-token',
      );

  @override
  Future<AuthSessionModel> login({
    required String username,
    required String password,
  }) async =>
      AuthSessionModel(
        playerId: 'fake-player',
        username: username,
        displayName: username,
        sessionToken: 'fake-token',
      );

  @override
  Future<void> logout() async {}

  @override
  Future<List<ChatMessageModel>> fetchRoomMessages(String roomId) async =>
      List.unmodifiable(chatMessages);

  @override
  Future<ChatMessageModel> sendRoomMessage({
    required String roomId,
    required String playerId,
    required String content,
  }) async {
    sentChatContents.add(content);
    final message = ChatMessageModel(
      messageId: 'message-${sentChatContents.length}',
      roomId: roomId,
      senderPlayerId: playerId,
      content: content,
      sentAt: DateTime.parse('2026-07-10T08:00:00Z'),
    );
    chatMessages.add(message);
    return message;
  }

  @override
  Future<List<GameRecordModel>> fetchPlayerRecords(String playerId) async =>
      const [];

  @override
  Future<RoomStateModel> createRoom(String playerId) async => RoomStateModel(
        roomId: 'room-1',
        phase: 'WAITING',
        ownerPlayerId: playerId,
        version: 1,
        seats: {'SOUTH': SeatInfo(playerId: playerId)},
      );

  @override
  Future<RoomStateModel> getRoom(String roomId) async {
    getRoomCalls += 1;
    return RoomStateModel(
      roomId: roomId,
      phase: 'WAITING',
      ownerPlayerId: 'fake-player',
      seats: {'SOUTH': const SeatInfo(playerId: 'fake-player')},
    );
  }

  @override
  Future<RoomStateModel> joinSeat(
          String roomId, String seat, String playerId) async =>
      RoomStateModel(
        roomId: roomId,
        phase: 'WAITING',
        ownerPlayerId: 'fake-player',
        seats: {
          'SOUTH': const SeatInfo(playerId: 'fake-player'),
          seat: SeatInfo(playerId: playerId),
        },
      );

  @override
  Future<RoomStateModel> leaveSeat(
          String roomId, String seat, String playerId) async =>
      RoomStateModel(
        roomId: roomId,
        phase: 'WAITING',
        ownerPlayerId: 'fake-player',
        seats: {},
      );

  @override
  Future<GameStateModel> startGame(String roomId, String playerId) async =>
      createdGame;

  @override
  Future<List<FriendshipModel>> fetchFriends(String playerId) async =>
      List.unmodifiable(friendships);

  @override
  Future<FriendshipModel> sendFriendRequest({
    required String requesterPlayerId,
    required String addresseePlayerId,
  }) async {
    final friendship = FriendshipModel(
      friendshipId: 'friendship-${friendships.length + 1}',
      requesterPlayerId: requesterPlayerId,
      addresseePlayerId: addresseePlayerId,
      status: 'PENDING',
    );
    friendships.add(friendship);
    return friendship;
  }

  @override
  Future<FriendshipModel> acceptFriendRequest({
    required String friendshipId,
    required String playerId,
  }) async =>
      const FriendshipModel(
        friendshipId: 'friendship-accepted',
        requesterPlayerId: 'friend-player',
        addresseePlayerId: 'fake-player',
        status: 'ACCEPTED',
      );

  @override
  Future<void> deleteFriendship({
    required String friendshipId,
    required String playerId,
  }) async {}

  @override
  Future<RoomInvitationModel> createRoomInvitation({
    required String roomId,
    required String fromPlayerId,
    required String toPlayerId,
  }) async {
    invitedPlayerIds.add(toPlayerId);
    final invitation = RoomInvitationModel(
      invitationId: 'invitation-${invitedPlayerIds.length}',
      roomId: roomId,
      fromPlayerId: fromPlayerId,
      toPlayerId: toPlayerId,
      status: 'PENDING',
      createdAt: DateTime.parse('2026-07-10T08:00:00Z'),
      expiresAt: DateTime.parse('2026-07-10T08:30:00Z'),
    );
    pendingInvitations.add(invitation);
    return invitation;
  }

  @override
  Future<List<RoomInvitationModel>> fetchPendingInvitations(
          String playerId) async =>
      List.unmodifiable(pendingInvitations);

  @override
  Future<RoomInvitationModel> respondToInvitation({
    required String invitationId,
    required String playerId,
    required bool accepted,
  }) async {
    respondedInvitations.add('$invitationId:$accepted');
    return RoomInvitationModel(
      invitationId: invitationId,
      roomId: 'room-1',
      fromPlayerId: 'friend-player',
      toPlayerId: playerId,
      status: accepted ? 'ACCEPTED' : 'DECLINED',
      createdAt: DateTime.parse('2026-07-10T08:00:00Z'),
      expiresAt: DateTime.parse('2026-07-10T08:30:00Z'),
    );
  }
}
