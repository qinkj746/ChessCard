import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:chess_card_app/api_client.dart';
import 'package:chess_card_app/auth_controller.dart';
import 'package:chess_card_app/auth_models.dart';
import 'package:chess_card_app/auth_session_storage.dart';
import 'package:chess_card_app/chat_models.dart';
import 'package:chess_card_app/friend_models.dart';
import 'package:chess_card_app/app_error.dart';
import 'package:chess_card_app/game_page.dart';
import 'package:chess_card_app/main.dart';
import 'package:chess_card_app/models.dart';
import 'package:chess_card_app/playing_card.dart';
import 'package:chess_card_app/record_models.dart';
import 'package:chess_card_app/room_connection.dart';
import 'package:chess_card_app/room_page.dart';
import 'package:chess_card_app/status_banner.dart';
import 'package:chess_card_app/table_layout.dart';
import 'package:chess_card_app/trick_animation.dart';

void main() {
  testWidgets('home page exposes single player and room entries',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      ChessCardApp(
          api: FakeApiClient(playGame), storage: MemoryAuthSessionStorage()),
    );
    await tester.pump();
    await tester.pump();

    expect(find.byIcon(Icons.play_arrow), findsOneWidget);
    expect(find.byIcon(Icons.meeting_room), findsOneWidget);
  });

  testWidgets('home shows a guest account card and opens authentication page',
      (WidgetTester tester) async {
    final api = FakeApiClient(playGame);
    await tester.pumpWidget(
      ChessCardApp(api: api, storage: MemoryAuthSessionStorage()),
    );
    await tester.pump();
    await tester.pump();

    expect(find.text('以访客身份游戏'), findsOneWidget);
    expect(find.text('登录 / 注册'), findsOneWidget);
    await tester.tap(find.text('登录 / 注册'));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('auth_username')), findsOneWidget);
  });

  testWidgets('home shows restored account and clears it on logout',
      (WidgetTester tester) async {
    final storage = MemoryAuthSessionStorage(const AuthSessionModel(
      playerId: 'account-1',
      username: 'alice',
      displayName: 'Alice',
      sessionToken: 'token-1',
    ));
    await tester.pumpWidget(
      ChessCardApp(api: FakeApiClient(playGame), storage: storage),
    );
    await tester.pump();
    await tester.pump();

    expect(find.text('Alice'), findsOneWidget);
    expect(find.text('退出登录'), findsOneWidget);
    await tester.tap(find.text('退出登录'));
    await tester.pumpAndSettle();

    expect(find.text('以访客身份游戏'), findsOneWidget);
    expect(storage.session, isNull);
  });

  testWidgets('room page renders account and seated player display names',
      (WidgetTester tester) async {
    final api = FakeApiClient(
      playGame,
      createdRoomSeats: const {
        'SOUTH': SeatInfo(playerId: 'account-1', displayName: 'Alice'),
        'NORTH': SeatInfo(playerId: 'account-2', displayName: 'Bob'),
      },
    );
    final controller = AuthController(
      api: api,
      storage: MemoryAuthSessionStorage(const AuthSessionModel(
        playerId: 'account-1',
        username: 'alice',
        displayName: 'Alice',
        sessionToken: 'token-1',
      )),
    );
    await controller.initialize();

    await tester.pumpWidget(MaterialApp(home: RoomPage(auth: controller)));
    await tester.pumpAndSettle();

    expect(find.text('\u73a9\u5bb6: Alice'), findsOneWidget);
    expect(find.textContaining('account-1'), findsNothing);

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();

    expect(find.text('Alice'), findsOneWidget);
    await tester.scrollUntilVisible(
      find.text('Bob'),
      200,
      scrollable: find.descendant(
        of: find.byType(GridView),
        matching: find.byType(Scrollable),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Bob'), findsOneWidget);
    expect(find.textContaining('account-1'), findsNothing);
    expect(find.textContaining('account-2'), findsNothing);
    expect(api.createdRoomPlayerIds, ['account-1']);
    expect(api.guestCreateCalls, 0);
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

  testWidgets('current trick and hand use playing cards',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(home: GamePage(initialGame: trickGame)),
    );

    expect(find.byType(PlayingCard), findsWidgets);
    expect(find.textContaining('K'), findsOneWidget);
    expect(find.text('♠'), findsNWidgets(4));
    expect(find.textContaining('黑桃'), findsNothing);
  });

  testWidgets('game page uses table layout container',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(home: GamePage(initialGame: playGame)),
    );

    expect(find.byType(TableLayout), findsOneWidget);
  });

  testWidgets('game table remains usable on a narrow screen',
      (WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(360, 520));
    addTearDown(() => tester.binding.setSurfaceSize(null));

    await tester.pumpWidget(
      const MaterialApp(home: GamePage(initialGame: trickGame)),
    );
    await tester.pump();

    expect(tester.takeException(), isNull);
    expect(find.byType(PlayingCard), findsWidgets);
    expect(find.textContaining('K'), findsOneWidget);
    expect(find.byIcon(Icons.send), findsOneWidget);
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

  testWidgets('room page shows lobby after guest init',
      (WidgetTester tester) async {
    final api = FakeApiClient(playGame);
    api.lobbyRooms.add(const RoomStateModel(
      roomId: 'room-open',
      phase: 'WAITING',
      ownerPlayerId: 'owner-player',
      version: 1,
      seats: {
        'SOUTH': SeatInfo(playerId: 'owner-player', displayName: 'Owner'),
      },
    ));

    await pumpRoomPage(tester, api: api);

    expect(api.fetchRoomsCalls, 1);
    expect(find.textContaining('room-open'), findsOneWidget);
    expect(find.byIcon(Icons.refresh), findsOneWidget);
    expect(find.byIcon(Icons.add), findsOneWidget);
  });

  testWidgets(
      'joining lobby room chooses first empty seat and opens room detail',
      (WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 1000));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final api = FakeApiClient(playGame);
    api.lobbyRooms.add(const RoomStateModel(
      roomId: 'room-open',
      phase: 'WAITING',
      ownerPlayerId: 'owner-player',
      version: 1,
      seats: {
        'SOUTH': SeatInfo(playerId: 'owner-player', displayName: 'Owner'),
        'WEST': SeatInfo(playerId: 'west-player', displayName: 'West'),
      },
    ));

    await pumpRoomPage(tester, api: api);
    await tester.tap(find.text('\u52a0\u5165').first);
    await tester.pumpAndSettle();

    expect(api.joinedSeats.last, 'NORTH');
    expect(find.textContaining('room-open'), findsOneWidget);
    expect(find.byType(GridView), findsOneWidget);
  });

  testWidgets('create room shows owner and start action',
      (WidgetTester tester) async {
    await pumpRoomPage(tester);

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.person), findsAtLeastNWidgets(1));
    expect(find.byIcon(Icons.play_arrow), findsOneWidget);
  });

  testWidgets('owner can add and remove a bot from each empty seat',
      (WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 1000));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final api = FakeApiClient(playGame);
    await pumpRoomPage(tester, api: api);

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();

    expect(find.text('添加人机'), findsNWidgets(3));

    const botSeats = ['WEST', 'NORTH', 'EAST'];
    for (final seat in botSeats) {
      await tester.tap(find.text('添加人机').first);
      await tester.pumpAndSettle();
      expect(api.addedBotSeats.last, seat);
    }

    expect(api.addedBotSeats, botSeats);
    expect(find.text('人机'), findsNWidgets(3));
    expect(
      find.descendant(
        of: find.ancestor(
          of: find.text('人机').first,
          matching: find.byType(Card),
        ),
        matching: find.byIcon(Icons.smart_toy),
      ),
      findsOneWidget,
    );
    expect(find.text('移除'), findsNWidgets(3));

    for (final seat in botSeats) {
      await tester.tap(find.text('移除').first);
      await tester.pumpAndSettle();
      expect(api.removedBotSeats.last, seat);
    }

    expect(api.removedBotSeats, botSeats);
    expect(find.text('添加人机'), findsNWidgets(3));
  });

  testWidgets('owner can start only after all four seats are occupied',
      (WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 1000));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final api = FakeApiClient(playGame);
    await pumpRoomPage(tester, api: api);

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();

    FilledButton startButton = tester.widget<FilledButton>(
      find.ancestor(
        of: find.byIcon(Icons.play_arrow),
        matching: find.byType(FilledButton),
      ),
    );
    expect(startButton.onPressed, isNull);

    for (final seat in ['WEST', 'NORTH', 'EAST']) {
      await tester.tap(find.text('添加人机').first);
      await tester.pumpAndSettle();
      expect(api.addedBotSeats.last, seat);
    }

    startButton = tester.widget<FilledButton>(
      find.ancestor(
        of: find.byIcon(Icons.play_arrow),
        matching: find.byType(FilledButton),
      ),
    );
    expect(startButton.onPressed, isNotNull);
  });

  testWidgets('stale bot response does not replace newer room snapshot',
      (WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 1000));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final events = FakeRoomEventSource();
    final api = FakeApiClient(playGame);
    final addBotCompleter = Completer<RoomStateModel>();
    api.addBotCompleter = addBotCompleter;
    api.getRoomResult = const RoomStateModel(
      roomId: 'room-1',
      phase: 'WAITING',
      ownerPlayerId: 'fake-player',
      version: 2,
      seats: {
        'SOUTH': SeatInfo(playerId: 'fake-player'),
        'WEST': SeatInfo(isBot: true, displayName: '浜烘満'),
        'NORTH': SeatInfo(isBot: true, displayName: '浜烘満'),
      },
    );
    await tester.pumpWidget(MaterialApp(
      home: RoomPage(
        api: api,
        roomConnectionFactory: (_) => events,
      ),
    ));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();
    await tester.tap(find.byIcon(Icons.smart_toy).first);
    await tester.pump();
    events.emit(const RoomEventModel(
      type: 'ROOM_UPDATED',
      roomId: 'room-1',
      version: 2,
    ));
    await tester.pump();

    addBotCompleter.complete(const RoomStateModel(
      roomId: 'room-1',
      phase: 'WAITING',
      ownerPlayerId: 'fake-player',
      version: 1,
      seats: {
        'SOUTH': SeatInfo(playerId: 'fake-player'),
        'WEST': SeatInfo(isBot: true, displayName: '浜烘満'),
      },
    ));
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.close), findsNWidgets(2));
  });

  testWidgets('playing room hides seat management actions',
      (WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 1000));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final api = FakeApiClient(
      playGame,
      createdRoomPhase: 'PLAYING',
      createdRoomSeats: const {
        'SOUTH': SeatInfo(playerId: 'fake-player'),
        'WEST': SeatInfo(isBot: true),
      },
    );
    await pumpRoomPage(tester, api: api);

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();

    expect(find.text('添加人机'), findsNothing);
    expect(find.text('移除'), findsNothing);
    expect(find.text('入座'), findsNothing);
    expect(find.text('离开'), findsNothing);
  });

  testWidgets('owner leaving empty waiting room returns to refreshed lobby',
      (WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 1000));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final api = FakeApiClient(playGame);
    final events = FakeRoomEventSource();
    await tester.pumpWidget(MaterialApp(
      home: RoomPage(
        api: api,
        roomConnectionFactory: (_) => events,
      ),
    ));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();
    api.lobbyRooms.add(const RoomStateModel(
      roomId: 'room-1',
      phase: 'WAITING',
      ownerPlayerId: 'fake-player',
      version: 1,
      seats: {
        'SOUTH': SeatInfo(playerId: 'fake-player'),
      },
    ));
    await tester.tap(find.text('\u79bb\u5f00'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 100));

    expect(api.leftSeats, ['SOUTH']);

    expect(api.fetchRoomsCalls, 2);
    expect(find.byIcon(Icons.add), findsOneWidget);
    expect(find.textContaining('room-1'), findsNothing);
  });

  testWidgets('back navigation releases the only human from a bot room',
      (WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 1000));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final api = FakeApiClient(
      playGame,
      createdRoomSeats: const {
        'SOUTH': SeatInfo(playerId: 'fake-player'),
        'WEST': SeatInfo(isBot: true),
        'NORTH': SeatInfo(isBot: true),
        'EAST': SeatInfo(isBot: true),
      },
    );
    await tester.pumpWidget(MaterialApp(
      home: Builder(
        builder: (context) => FilledButton(
          onPressed: () => Navigator.of(context).push(
            MaterialPageRoute(builder: (_) => RoomPage(api: api)),
          ),
          child: const Text('open room page'),
        ),
      ),
    ));

    await tester.tap(find.text('open room page'));
    await tester.pumpAndSettle();
    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();

    await tester.pageBack();
    await tester.pumpAndSettle();

    expect(api.leftSeats, ['SOUTH']);
    expect(find.text('open room page'), findsOneWidget);
  });
  testWidgets('playing room disables start even when all seats are occupied',
      (WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 1000));
    addTearDown(() => tester.binding.setSurfaceSize(null));

    final api = FakeApiClient(
      playGame,
      createdRoomPhase: 'PLAYING',
      createdRoomSeats: const {
        'SOUTH': SeatInfo(playerId: 'fake-player'),
        'WEST': SeatInfo(isBot: true, displayName: '浜烘満'),
        'NORTH': SeatInfo(isBot: true, displayName: '浜烘満'),
        'EAST': SeatInfo(playerId: 'player-east'),
      },
    );
    await pumpRoomPage(tester, api: api);

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();

    final startButton = tester.widget<FilledButton>(
      find.ancestor(
        of: find.byIcon(Icons.play_arrow),
        matching: find.byType(FilledButton),
      ),
    );
    expect(startButton.onPressed, isNull);
  });

  testWidgets('unseated non-owner can join only truly empty seats',
      (WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 1000));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final api = FakeApiClient(
      playGame,
      createdRoomOwnerPlayerId: 'owner-player',
      createdRoomSeats: const {
        'SOUTH': SeatInfo(playerId: 'owner-player', displayName: 'Owner'),
        'WEST': SeatInfo(isBot: true),
      },
    );
    await pumpRoomPage(tester, api: api);

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();

    expect(find.text('添加人机'), findsNothing);
    expect(find.text('移除'), findsNothing);
    expect(find.text('入座'), findsNWidgets(2));
    expect(find.byIcon(Icons.smart_toy), findsOneWidget);
  });

  testWidgets('unseated owner can join or add a bot to every empty seat',
      (WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(320, 1000));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final api = FakeApiClient(
      playGame,
      createdRoomOwnerPlayerId: 'fake-player',
      createdRoomSeats: const {},
    );
    await pumpRoomPage(tester, api: api);

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();

    expect(find.text('入座'), findsNWidgets(4));
    expect(find.byTooltip('添加人机'), findsNWidgets(4));

    await tester.tap(find.byTooltip('添加人机').first);
    await tester.pumpAndSettle();

    expect(api.addedBotSeats, ['SOUTH']);
    expect(
      find.descendant(
        of: find.ancestor(
          of: find.text('人机'),
          matching: find.byType(Card),
        ),
        matching: find.byIcon(Icons.smart_toy),
      ),
      findsOneWidget,
    );
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

  testWidgets('room page deduplicates chat event before send response',
      (WidgetTester tester) async {
    final events = FakeRoomEventSource();
    final api = FakeApiClient(playGame);
    final completer = Completer<ChatMessageModel>();
    api.sendRoomMessageCompleter = completer;
    await tester.pumpWidget(MaterialApp(
      home: RoomPage(
        api: api,
        roomConnectionFactory: (_) => events,
      ),
    ));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), 'hello once');
    await tester.tap(find.byIcon(Icons.send));
    await tester.pump();

    events.emit(const RoomEventModel(
      type: 'CHAT_MESSAGE',
      roomId: 'room-1',
      payload: {
        'messageId': 'message-1',
        'roomId': 'room-1',
        'senderPlayerId': 'fake-player',
        'content': 'hello once',
        'sentAt': '2026-07-10T08:00:00Z',
      },
    ));
    await tester.pump();
    completer.complete(ChatMessageModel(
      messageId: 'message-1',
      roomId: 'room-1',
      senderPlayerId: 'fake-player',
      content: 'hello once',
      sentAt: DateTime.parse('2026-07-10T08:00:00Z'),
    ));
    await tester.pumpAndSettle();

    expect(api.sentChatContents, ['hello once']);
    expect(find.text('hello once'), findsOneWidget);
    final field = tester.widget<TextField>(find.byType(TextField));
    expect(field.controller?.text, isEmpty);
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
    final api = FakeApiClient(
      playGame,
      createdRoomSeats: const {
        'SOUTH': SeatInfo(playerId: 'fake-player'),
        'WEST': SeatInfo(playerId: 'player-west'),
        'NORTH': SeatInfo(playerId: 'player-north'),
        'EAST': SeatInfo(playerId: 'player-east'),
      },
    );
    await pumpRoomPage(tester, api: api);

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.play_arrow));
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.send), findsOneWidget);
  });

  testWidgets('owner exiting room game deleted by server returns to lobby',
      (WidgetTester tester) async {
    final api = FakeApiClient(
      playGame,
      createdRoomSeats: const {
        'SOUTH': SeatInfo(playerId: 'fake-player'),
        'WEST': SeatInfo(isBot: true, displayName: '人机'),
        'NORTH': SeatInfo(isBot: true, displayName: '人机'),
        'EAST': SeatInfo(isBot: true, displayName: '人机'),
      },
      getRoomError: GameApiException(
        code: 'ROOM_NOT_FOUND',
        message: 'room not found',
      ),
    );
    await pumpRoomPage(tester, api: api);

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();
    await tester.tap(find.byIcon(Icons.play_arrow));
    await tester.pumpAndSettle();
    await tester.tap(find.byIcon(Icons.arrow_back));
    await tester.pumpAndSettle();
    await tester.tap(find.text('退出'));
    await tester.pumpAndSettle();

    expect(api.leavePlayingRoomCalls, 1);
    expect(api.fetchRoomsCalls, 2);
    expect(find.byIcon(Icons.add), findsOneWidget);
    expect(find.textContaining('room-1'), findsNothing);
    expect(find.text('room not found'), findsNothing);
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

  testWidgets('active room game confirms before exiting',
      (WidgetTester tester) async {
    final api = FakeApiClient(playGame);
    await tester.pumpWidget(MaterialApp(
      home: GamePage(
        initialGame: playGame,
        api: api,
        playerId: 'fake-player',
        roomId: 'room-1',
      ),
    ));

    await tester.tap(find.byIcon(Icons.arrow_back));
    await tester.pumpAndSettle();

    expect(find.text('游戏正在进行'), findsOneWidget);
    expect(find.text('继续游戏'), findsOneWidget);
    expect(find.text('退出'), findsOneWidget);
  });

  testWidgets('canceling active room game exit stays on game page',
      (WidgetTester tester) async {
    final api = FakeApiClient(playGame);
    await tester.pumpWidget(MaterialApp(
      home: GamePage(
        initialGame: playGame,
        api: api,
        playerId: 'fake-player',
        roomId: 'room-1',
      ),
    ));

    await tester.tap(find.byIcon(Icons.arrow_back));
    await tester.pumpAndSettle();
    await tester.tap(find.text('继续游戏'));
    await tester.pumpAndSettle();

    expect(find.text('游戏正在进行'), findsNothing);
    expect(find.byType(GamePage), findsOneWidget);
    expect(api.leavePlayingRoomCalls, 0);
  });

  testWidgets('confirming active room game exit calls API and returns',
      (WidgetTester tester) async {
    final api = FakeApiClient(playGame);
    Object? poppedResult;
    await tester.pumpWidget(MaterialApp(
      home: Builder(
        builder: (context) => FilledButton(
          onPressed: () async {
            poppedResult = await Navigator.of(context).push<String>(
              MaterialPageRoute(
                builder: (_) => GamePage(
                  initialGame: playGame,
                  api: api,
                  playerId: 'fake-player',
                  roomId: 'room-1',
                ),
              ),
            );
          },
          child: const Text('open'),
        ),
      ),
    ));
    await tester.tap(find.text('open'));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.arrow_back));
    await tester.pumpAndSettle();
    await tester.tap(find.text('退出'));
    await tester.pumpAndSettle();

    expect(api.leavePlayingRoomCalls, 1);
    expect(api.leftPlayingRoomPlayerIds, ['fake-player']);
    expect(poppedResult, playGame.id);
  });

  testWidgets('active room game exit failure stays and shows error',
      (WidgetTester tester) async {
    final api = FakeApiClient(playGame, leavePlayingRoomError: 'exit failed');
    await tester.pumpWidget(MaterialApp(
      home: GamePage(
        initialGame: playGame,
        api: api,
        playerId: 'fake-player',
        roomId: 'room-1',
      ),
    ));

    await tester.tap(find.byIcon(Icons.arrow_back));
    await tester.pumpAndSettle();
    await tester.tap(find.text('退出'));
    await tester.pumpAndSettle();

    expect(find.text('exit failed'), findsOneWidget);
    expect(find.byType(GamePage), findsOneWidget);
  });

  testWidgets('finished room game returns without in-progress confirmation',
      (WidgetTester tester) async {
    final api = FakeApiClient(finishedGame);
    await tester.pumpWidget(MaterialApp(
      home: GamePage(
        initialGame: finishedGame,
        api: api,
        playerId: 'fake-player',
        roomId: 'room-1',
      ),
    ));

    await tester.tap(find.byIcon(Icons.arrow_back));
    await tester.pumpAndSettle();

    expect(find.text('游戏正在进行'), findsNothing);
    expect(api.leavePlayingRoomCalls, 0);
  });

  testWidgets('settled trick shows a winner animation',
      (WidgetTester tester) async {
    final api = FakeApiClient(settledTrickGame);
    await tester.pumpWidget(
      MaterialApp(
        home: GamePage(
          initialGame: fullCurrentTrickGame,
          api: api,
          animationsEnabled: true,
        ),
      ),
    );

    await tester.tap(find.byIcon(Icons.smart_toy));
    await tester.pump();
    await tester.pump();

    expect(find.byType(TrickAnimation), findsOneWidget);
    expect(find.text('西 赢得本墩'), findsOneWidget);
  });

  testWidgets('trick animation can be disabled', (WidgetTester tester) async {
    final api = FakeApiClient(settledTrickGame);
    await tester.pumpWidget(
      MaterialApp(
        home: GamePage(
          initialGame: fullCurrentTrickGame,
          api: api,
          animationsEnabled: false,
        ),
      ),
    );

    await tester.tap(find.byIcon(Icons.smart_toy));
    await tester.pump();
    await tester.pump();

    expect(find.byType(TrickAnimation), findsNothing);
    expect(find.text('西 赢得本墩'), findsNothing);
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

const fullCurrentTrickGame = GameStateModel(
  id: 'game-9',
  phase: 'PLAY',
  levelRank: 'ACE',
  trumpSuit: 'HEART',
  banker: 'SOUTH',
  currentTurn: 'EAST',
  attackerScore: 0,
  winningTeam: null,
  levelDelta: 0,
  nextLevelRank: null,
  completed: false,
  handCounts: {'SOUTH': 24, 'WEST': 24, 'NORTH': 24, 'EAST': 24},
  southHand: [CardModel(suit: 'SPADE', rank: 'FIVE', deckIndex: 0)],
  kitty: [],
  currentTrick: {
    'SOUTH': [CardModel(suit: 'SPADE', rank: 'KING', deckIndex: 0)],
    'WEST': [CardModel(suit: 'SPADE', rank: 'ACE', deckIndex: 0)],
    'NORTH': [CardModel(suit: 'SPADE', rank: 'QUEEN', deckIndex: 0)],
    'EAST': [CardModel(suit: 'SPADE', rank: 'JACK', deckIndex: 0)],
  },
  currentTrickPlays: [
    TrickPlayModel(
      seat: 'SOUTH',
      cards: [CardModel(suit: 'SPADE', rank: 'KING', deckIndex: 0)],
    ),
    TrickPlayModel(
      seat: 'WEST',
      cards: [CardModel(suit: 'SPADE', rank: 'ACE', deckIndex: 0)],
    ),
    TrickPlayModel(
      seat: 'NORTH',
      cards: [CardModel(suit: 'SPADE', rank: 'QUEEN', deckIndex: 0)],
    ),
    TrickPlayModel(
      seat: 'EAST',
      cards: [CardModel(suit: 'SPADE', rank: 'JACK', deckIndex: 0)],
    ),
  ],
  declarationOptions: [],
);

const settledTrickGame = GameStateModel(
  id: 'game-9',
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
  handCounts: {'SOUTH': 24, 'WEST': 24, 'NORTH': 24, 'EAST': 24},
  southHand: [CardModel(suit: 'SPADE', rank: 'FIVE', deckIndex: 0)],
  kitty: [],
  currentTrick: {},
  currentTrickPlays: [],
  playedTricks: [
    PlayedTrickModel(
      index: 1,
      leader: 'SOUTH',
      winner: 'WEST',
      points: 0,
      plays: [
        TrickPlayModel(
          seat: 'SOUTH',
          cards: [CardModel(suit: 'SPADE', rank: 'KING', deckIndex: 0)],
        ),
        TrickPlayModel(
          seat: 'WEST',
          cards: [CardModel(suit: 'SPADE', rank: 'ACE', deckIndex: 0)],
        ),
        TrickPlayModel(
          seat: 'NORTH',
          cards: [CardModel(suit: 'SPADE', rank: 'QUEEN', deckIndex: 0)],
        ),
        TrickPlayModel(
          seat: 'EAST',
          cards: [CardModel(suit: 'SPADE', rank: 'JACK', deckIndex: 0)],
        ),
      ],
    ),
  ],
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
    this.createdRoomSeats,
    this.createdRoomOwnerPlayerId,
    this.createdRoomPhase = 'WAITING',
    this.playError,
    this.leavePlayingRoomError,
    this.getRoomError,
  })  : nextGameResult = nextGame ?? createdGame,
        refreshedGameResult = refreshedGame ?? createdGame;

  final GameStateModel createdGame;
  final GameStateModel nextGameResult;
  final GameStateModel refreshedGameResult;
  final Map<String, SeatInfo>? createdRoomSeats;
  final String? createdRoomOwnerPlayerId;
  final String createdRoomPhase;
  final String? playError;
  final String? leavePlayingRoomError;
  final GameApiException? getRoomError;
  final List<ChatMessageModel> chatMessages = [];
  final List<String> sentChatContents = [];
  Completer<ChatMessageModel>? sendRoomMessageCompleter;
  final List<FriendshipModel> friendships = [];
  final List<RoomInvitationModel> pendingInvitations = [];
  final List<RoomStateModel> lobbyRooms = [];
  final List<String> invitedPlayerIds = [];
  final List<String> respondedInvitations = [];
  final List<String> joinedSeats = [];
  final Map<String, SeatInfo> roomSeats = {};
  int nextGameCalls = 0;
  int getRoomCalls = 0;
  int getGameCalls = 0;
  int guestCreateCalls = 0;
  int fetchRoomsCalls = 0;
  int roomVersion = 0;
  final List<String> createdRoomPlayerIds = [];
  final List<String> requestedGameIds = [];
  final List<String> addedBotSeats = [];
  final List<String> removedBotSeats = [];
  final List<String> leftSeats = [];
  final List<String> leftPlayingRoomPlayerIds = [];
  int leavePlayingRoomCalls = 0;
  RoomStateModel? getRoomResult;
  Completer<RoomStateModel>? addBotCompleter;

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
  Future<PlayerProfileModel> createGuestPlayer() async {
    guestCreateCalls += 1;
    return const PlayerProfileModel(
      playerId: 'fake-player',
      displayName: 'Guest-0001',
      guest: true,
      sessionToken: 'fake-token',
    );
  }

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
  void setSessionToken(String? sessionToken) {}

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
    final completer = sendRoomMessageCompleter;
    if (completer != null) {
      final message = await completer.future;
      chatMessages.add(message);
      return message;
    }
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
  Future<List<RoomStateModel>> fetchRooms() async {
    fetchRoomsCalls += 1;
    return List.unmodifiable(lobbyRooms);
  }

  @override
  Future<RoomStateModel> createRoom(String playerId) async {
    createdRoomPlayerIds.add(playerId);
    roomVersion = 1;
    roomSeats
      ..clear()
      ..addAll(
        createdRoomSeats ?? {'SOUTH': SeatInfo(playerId: playerId)},
      );
    return RoomStateModel(
      roomId: 'room-1',
      phase: createdRoomPhase,
      ownerPlayerId: createdRoomOwnerPlayerId ?? playerId,
      version: 1,
      seats: Map.unmodifiable(roomSeats),
    );
  }

  @override
  Future<RoomStateModel> getRoom(String roomId) async {
    getRoomCalls += 1;
    if (getRoomError != null) throw getRoomError!;
    final result = getRoomResult;
    if (result != null) return result;
    return RoomStateModel(
      roomId: roomId,
      phase: 'WAITING',
      ownerPlayerId: createdRoomOwnerPlayerId ?? 'fake-player',
      version: roomVersion,
      seats: Map.unmodifiable(roomSeats),
    );
  }

  @override
  Future<RoomStateModel> joinSeat(
      String roomId, String seat, String playerId) async {
    joinedSeats.add(seat);
    roomSeats[seat] = SeatInfo(playerId: playerId);
    roomVersion += 1;
    return RoomStateModel(
      roomId: roomId,
      phase: 'WAITING',
      ownerPlayerId: createdRoomOwnerPlayerId ?? 'fake-player',
      version: roomVersion,
      seats: Map.unmodifiable(roomSeats),
    );
  }

  @override
  Future<RoomStateModel> leaveSeat(
      String roomId, String seat, String playerId) async {
    leftSeats.add(seat);
    roomSeats.remove(seat);
    roomVersion += 1;
    return RoomStateModel(
      roomId: roomId,
      phase: 'WAITING',
      ownerPlayerId: createdRoomOwnerPlayerId ?? 'fake-player',
      version: roomVersion,
      seats: Map.unmodifiable(roomSeats),
    );
  }

  @override
  Future<RoomStateModel> leavePlayingRoom(
      String roomId, String playerId) async {
    leavePlayingRoomCalls += 1;
    leftPlayingRoomPlayerIds.add(playerId);
    if (leavePlayingRoomError != null) {
      throw Exception(leavePlayingRoomError);
    }
    roomVersion += 1;
    roomSeats.updateAll((seat, info) {
      if (!info.isBot && info.playerId == playerId) {
        return const SeatInfo(isBot: true, displayName: '人机');
      }
      return info;
    });
    return RoomStateModel(
      roomId: roomId,
      phase: 'PLAYING',
      ownerPlayerId: createdRoomOwnerPlayerId ?? 'fake-player',
      version: roomVersion,
      seats: Map.unmodifiable(roomSeats),
    );
  }

  @override
  Future<RoomStateModel> addBot(
      String roomId, String seat, String playerId) async {
    addedBotSeats.add(seat);
    final completer = addBotCompleter;
    if (completer != null) return completer.future;
    roomSeats[seat] = const SeatInfo(isBot: true, displayName: '人机');
    roomVersion += 1;
    return RoomStateModel(
      roomId: roomId,
      phase: 'WAITING',
      ownerPlayerId: createdRoomOwnerPlayerId ?? playerId,
      version: roomVersion,
      seats: Map.unmodifiable(roomSeats),
    );
  }

  @override
  Future<RoomStateModel> removeBot(
      String roomId, String seat, String playerId) async {
    removedBotSeats.add(seat);
    roomSeats.remove(seat);
    roomVersion += 1;
    return RoomStateModel(
      roomId: roomId,
      phase: 'WAITING',
      ownerPlayerId: createdRoomOwnerPlayerId ?? playerId,
      version: roomVersion,
      seats: Map.unmodifiable(roomSeats),
    );
  }

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

class MemoryAuthSessionStorage implements AuthSessionStorage {
  MemoryAuthSessionStorage([this.session]);

  AuthSessionModel? session;

  @override
  Future<void> clear() async {
    session = null;
  }

  @override
  Future<AuthSessionModel?> read() async => session;

  @override
  Future<void> write(AuthSessionModel value) async {
    session = value;
  }
}
