import 'dart:async';

import 'package:flutter/material.dart';

import 'api_client.dart';
import 'app_error.dart';
import 'models.dart';
import 'room_connection.dart';
import 'status_banner.dart';

class GamePage extends StatefulWidget {
  const GamePage({
    super.key,
    this.initialGame,
    this.api,
    this.playerId,
    this.roomId,
    this.roomEvents,
  });

  final GameStateModel? initialGame;
  final GameApi? api;
  final String? playerId;
  final String? roomId;
  final RoomEventSource? roomEvents;

  bool get isRoomMode => roomId != null && playerId != null;

  @override
  State<GamePage> createState() => _GamePageState();
}

class _GamePageState extends State<GamePage> {
  late final GameApi api;
  final Set<CardModel> selected = {};
  GameStateModel? game;
  AppError? error;
  bool loading = false;
  VoidCallback? _lastAction;
  String? _actionMessage;
  bool _actionMessageVisible = false;
  Timer? _messageTimer;
  RoomEventSource? _roomEvents;
  StreamSubscription<RoomEventModel>? _roomEventSubscription;
  int _lastRoomEventVersion = 0;

  String? get _playerId => widget.isRoomMode ? widget.playerId : null;

  @override
  void initState() {
    super.initState();
    api = widget.api ?? ApiClient();
    game = widget.initialGame;
    _updateActionMessage(game);
    _attachRoomEvents();
  }

  @override
  void dispose() {
    _messageTimer?.cancel();
    _roomEventSubscription?.cancel();
    _roomEvents?.disconnect();
    super.dispose();
  }

  void _attachRoomEvents() {
    final roomId = widget.roomId;
    if (!widget.isRoomMode || roomId == null) return;
    final source = widget.roomEvents ??
        RoomConnection(baseUrl: _baseUrl(), roomId: roomId);
    _roomEvents = source;
    _roomEventSubscription = source.events.listen(
      _handleRoomEvent,
      onError: (Object e) {
        if (mounted) setState(() => error = AppError.fromException(e));
      },
    );
    source.connect();
  }

  Future<void> _handleRoomEvent(RoomEventModel event) async {
    if (!widget.isRoomMode || event.type != 'GAME_UPDATED') return;
    if (event.roomId != widget.roomId) return;
    final version = event.version;
    if (version != null && version <= _lastRoomEventVersion) return;
    final gameId = event.gameId ?? game?.id;
    if (gameId == null) return;
    try {
      final next = await api.getGame(gameId);
      if (!mounted) return;
      setState(() {
        game = next;
        selected.clear();
        _updateActionMessage(next);
        if (version != null) _lastRoomEventVersion = version;
      });
    } catch (e) {
      if (mounted) setState(() => error = AppError.fromException(e));
    }
  }

  String _baseUrl() =>
      api is ApiClient ? (api as ApiClient).baseUrl : 'http://localhost:8080';
  void _updateActionMessage(GameStateModel? g) {
    final msg = g?.lastActionMessage;
    if (msg != null && msg.isNotEmpty) {
      _messageTimer?.cancel();
      _actionMessage = msg;
      _actionMessageVisible = true;
      _messageTimer = Timer(const Duration(seconds: 2), () {
        if (mounted) setState(() => _actionMessageVisible = false);
      });
    } else {
      _actionMessage = null;
      _actionMessageVisible = false;
    }
  }

  Future<void> _run(Future<GameStateModel> Function() action,
      {VoidCallback? retryAction}) async {
    setState(() {
      loading = true;
      error = null;
      _lastAction = retryAction;
    });
    try {
      final next = await action();
      setState(() {
        game = next;
        selected.clear();
        _lastAction = null;
        _updateActionMessage(next);
      });
    } catch (e) {
      setState(() => error = AppError.fromException(e));
    } finally {
      setState(() => loading = false);
    }
  }

  void _retryLastAction() {
    final action = _lastAction;
    if (action != null) {
      setState(() => error = null);
      action();
    }
  }

  @override
  Widget build(BuildContext context) {
    final current = game;
    return Scaffold(
      appBar: AppBar(
        title: const Text('\u5347\u7ea7\u724c\u5c40'),
        actions: [
          if (current != null)
            IconButton(
              tooltip: widget.isRoomMode
                  ? '\u8fd4\u56de\u623f\u95f4'
                  : '\u91cd\u65b0\u5f00\u5c40',
              onPressed: loading
                  ? null
                  : widget.isRoomMode
                      ? () => Navigator.of(context).pop(current.id)
                      : () => _run(api.createGame),
              icon: Icon(widget.isRoomMode ? Icons.arrow_back : Icons.refresh),
            ),
        ],
      ),
      body: current == null ? _buildStart() : _buildTable(current),
    );
  }

  Widget _buildStart() {
    if (widget.isRoomMode) {
      return const Center(
          child: Text(
              '\u6b63\u5728\u7b49\u5f85\u623f\u95f4\u724c\u5c40\u6570\u636e'));
    }
    return Center(
      child: FilledButton.icon(
        onPressed: loading ? null : () => _run(api.createGame),
        icon: const Icon(Icons.play_arrow),
        label: Text(
            loading ? '\u521b\u5efa\u4e2d...' : '\u521b\u5efa\u6e38\u620f'),
      ),
    );
  }

  Widget _buildTable(GameStateModel game) {
    return Column(
      children: [
        _buildStatus(game),
        if (error != null)
          StatusBanner(
              error: error!,
              onRetry: _lastAction != null ? _retryLastAction : null),
        if (_actionMessage != null)
          AnimatedOpacity(
            opacity: _actionMessageVisible ? 1.0 : 0.0,
            duration: const Duration(milliseconds: 500),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
              child: Text(
                _actionMessage!,
                style: TextStyle(
                    color: Colors.orange.shade800,
                    fontSize: 13,
                    fontWeight: FontWeight.w500),
              ),
            ),
          ),
        Expanded(child: _buildBoard(game)),
        if (game.playedTricks.isNotEmpty) _buildPlayedHistory(game),
        _buildActions(game),
        _buildHand(game.southHand),
      ],
    );
  }

  Widget _buildStatus(GameStateModel game) {
    return Padding(
      padding: const EdgeInsets.all(12),
      child: Wrap(
        spacing: 12,
        runSpacing: 8,
        children: [
          _chip('\u9636\u6bb5', _phase(game.phase)),
          _chip('\u7ea7\u724c', _rank(game.levelRank)),
          _chip('\u4e3b\u82b1\u8272',
              game.trumpSuit == null ? '\u672a\u5b9a' : _suit(game.trumpSuit!)),
          _chip('\u5e84\u5bb6', _seat(game.banker)),
          _chip('\u5f53\u524d', _seat(game.currentTurn)),
          _chip('\u653b\u65b9\u5206', '${game.attackerScore}'),
          if (game.phase == 'FINISHED')
            _chip('\u80dc\u5229\u65b9', _team(game.winningTeam)),
          if (game.phase == 'FINISHED')
            _chip('\u5347\u7ea7', '${game.levelDelta}'),
          if (game.phase == 'FINISHED')
            _chip(
                '\u4e0b\u4e00\u7ea7',
                game.nextLevelRank == null
                    ? '\u672a\u5b9a'
                    : _rank(game.nextLevelRank!)),
          if (game.phase == 'FINISHED')
            _chip('\u5df2\u5b8c\u6210', game.completed ? '\u662f' : '\u5426'),
        ],
      ),
    );
  }

  Widget _chip(String label, String value) {
    return Chip(label: Text('$label: $value'));
  }

  Widget _buildBoard(GameStateModel game) {
    return Container(
      margin: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFF0F6B45),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Stack(
        children: [
          Align(
              alignment: Alignment.topCenter,
              child: _opponent('\u5317', game.handCounts['NORTH'] ?? 0)),
          Align(
              alignment: Alignment.centerLeft,
              child: _opponent('\u897f', game.handCounts['WEST'] ?? 0)),
          Align(
              alignment: Alignment.centerRight,
              child: _opponent('\u4e1c', game.handCounts['EAST'] ?? 0)),
          Center(child: _buildTrick(game)),
        ],
      ),
    );
  }

  Widget _buildTrick(GameStateModel game) {
    final currentPlays = game.currentTrickPlays.isNotEmpty
        ? game.currentTrickPlays
        : _legacyCurrentTrickPlays(game);
    if (currentPlays.isEmpty) {
      if (game.playedTricks.isNotEmpty && game.phase == 'PLAY') {
        return _completedTrickPreview(game.playedTricks.last);
      }
      return Text(
        game.phase == 'DECLARE'
            ? '\u7b49\u5f85\u53eb\u4e3b'
            : game.phase == 'KITTY'
                ? '\u7b49\u5f85\u6263\u5e95'
                : '\u7b49\u5f85\u51fa\u724c',
        style: Theme.of(context)
            .textTheme
            .headlineMedium
            ?.copyWith(color: Colors.white),
      );
    }
    return _trickPlayWrap(currentPlays);
  }

  List<TrickPlayModel> _legacyCurrentTrickPlays(GameStateModel game) {
    const seats = ['NORTH', 'WEST', 'SOUTH', 'EAST'];
    return seats
        .where((seat) => game.currentTrick.containsKey(seat))
        .map((seat) =>
            TrickPlayModel(seat: seat, cards: game.currentTrick[seat]!))
        .toList();
  }

  Widget _completedTrickPreview(PlayedTrickModel trick) {
    return FittedBox(
      fit: BoxFit.scaleDown,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            '\u7b2c ${trick.index} \u58a9\u5b8c\u6210 · \u8d62\u5bb6 ${_seat(trick.winner)}',
            style: const TextStyle(
                color: Colors.white, fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 10),
          _trickPlayWrap(trick.plays),
        ],
      ),
    );
  }

  Widget _trickPlayWrap(List<TrickPlayModel> plays) {
    return ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: 420),
      child: Wrap(
        alignment: WrapAlignment.center,
        spacing: 10,
        runSpacing: 10,
        children:
            plays.map((play) => _trickSeat(play.seat, play.cards)).toList(),
      ),
    );
  }

  Widget _trickSeat(String seat, List<CardModel> cards) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.14),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.white24),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(_seat(seat),
                style: const TextStyle(
                    color: Colors.white, fontWeight: FontWeight.w700)),
            const SizedBox(height: 6),
            Wrap(
              spacing: 4,
              children: cards
                  .map(
                    (card) => Container(
                      width: 42,
                      height: 56,
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(6),
                      ),
                      alignment: Alignment.center,
                      child: Text(
                        '${_suit(card.suit)}\n${_rank(card.rank)}',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: _isRed(card.suit) ? Colors.red : Colors.black,
                          fontSize: 12,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                  )
                  .toList(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _opponent(String name, int count) {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: Colors.black.withValues(alpha: 0.2),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
          child: Text('$name  $count \u5f20',
              style: const TextStyle(color: Colors.white)),
        ),
      ),
    );
  }

  Widget _buildPlayedHistory(GameStateModel game) {
    return SizedBox(
      height: 152,
      child: ListView.separated(
        padding: const EdgeInsets.symmetric(horizontal: 12),
        scrollDirection: Axis.horizontal,
        itemCount: game.playedTricks.length,
        separatorBuilder: (_, __) => const SizedBox(width: 8),
        itemBuilder: (context, index) {
          final trick = game.playedTricks[index];
          return Container(
            width: 240,
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: Colors.green.shade50,
              border: Border.all(color: Colors.green.shade200),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('\u7b2c ${trick.index} \u58a9',
                    style: const TextStyle(fontWeight: FontWeight.w700)),
                const SizedBox(height: 4),
                ...trick.plays.map(
                  (play) => Text(
                      '${_seat(play.seat)}: ${play.cards.map(_cardText).join(' ')}'),
                ),
                const Spacer(),
                Text('\u8d62\u5bb6: ${_seat(trick.winner)}',
                    style: const TextStyle(fontSize: 12)),
              ],
            ),
          );
        },
      ),
    );
  }

  String _cardText(CardModel card) => '${_suit(card.suit)} ${_rank(card.rank)}';

  Widget _buildActions(GameStateModel game) {
    return Padding(
      padding: const EdgeInsets.all(12),
      child: Wrap(
        spacing: 8,
        runSpacing: 8,
        alignment: WrapAlignment.center,
        children: [
          if (game.phase == 'DECLARE') ..._declareButtons(game),
          if (game.phase == 'KITTY')
            FilledButton.icon(
              onPressed: !loading && selected.length == 8
                  ? () {
                      Future<GameStateModel> action() =>
                          api.kitty(game.id, selected.toList(),
                              playerId: _playerId);
                      _run(action, retryAction: () => _run(action));
                    }
                  : null,
              icon: const Icon(Icons.inventory_2),
              label: Text('\u6263\u5e95 ${selected.length}/8'),
            ),
          if (game.phase == 'PLAY') ...[
            FilledButton.icon(
              onPressed: !loading && selected.isNotEmpty
                  ? () {
                      Future<GameStateModel> action() =>
                          api.play(game.id, selected.toList(),
                              playerId: _playerId);
                      _run(action, retryAction: () => _run(action));
                    }
                  : null,
              icon: const Icon(Icons.send),
              label: Text('\u51fa\u724c ${selected.length}'),
            ),
            OutlinedButton.icon(
              onPressed: loading
                  ? null
                  : () {
                      Future<GameStateModel> action() => api.aiStep(game.id);
                      _run(action, retryAction: () => _run(action));
                    },
              icon: const Icon(Icons.smart_toy),
              label: const Text('\u63a8\u8fdb AI'),
            ),
          ],
          if (game.phase == 'FINISHED' && !game.completed)
            widget.isRoomMode
                ? FilledButton.icon(
                    onPressed: loading
                        ? null
                        : () => Navigator.of(context).pop(game.id),
                    icon: const Icon(Icons.arrow_back),
                    label: const Text('\u8fd4\u56de\u623f\u95f4'),
                  )
                : FilledButton.icon(
                    onPressed: loading
                        ? null
                        : () {
                            Future<GameStateModel> action() =>
                                api.nextGame(game.id);
                            _run(action, retryAction: () => _run(action));
                          },
                    icon: const Icon(Icons.skip_next),
                    label: const Text('\u4e0b\u4e00\u5c40'),
                  ),
          if (selected.isNotEmpty)
            TextButton.icon(
              onPressed: loading ? null : () => setState(selected.clear),
              icon: const Icon(Icons.clear),
              label: const Text('\u6e05\u7a7a\u9009\u62e9'),
            ),
        ],
      ),
    );
  }

  List<Widget> _declareButtons(GameStateModel game) {
    if (game.currentTurn != 'SOUTH' ||
        (game.banker != null && game.banker != 'SOUTH')) {
      return [
        const Text('\u7b49\u5f85 AI \u53eb\u4e3b'),
        OutlinedButton.icon(
          onPressed: loading
              ? null
              : () {
                  Future<GameStateModel> action() => api.aiStep(game.id);
                  _run(action, retryAction: () => _run(action));
                },
          icon: const Icon(Icons.smart_toy),
          label: const Text('\u63a8\u8fdb AI'),
        ),
      ];
    }
    if (game.declarationOptions.isEmpty) {
      return [
        const Text(
            '\u5f53\u524d\u6ca1\u6709\u53ef\u53eb\u4e3b\u7684\u82b1\u8272'),
        OutlinedButton.icon(
          onPressed: loading
              ? null
              : () {
                  Future<GameStateModel> action() => api.aiStep(game.id);
                  _run(action, retryAction: () => _run(action));
                },
          icon: const Icon(Icons.smart_toy),
          label: const Text('\u63a8\u8fdb AI'),
        ),
      ];
    }
    return game.declarationOptions
        .map(
          (suit) => FilledButton.icon(
            onPressed: loading
                ? null
                : () {
                    Future<GameStateModel> action() =>
                        api.declare(game.id, suit, playerId: _playerId);
                    _run(action, retryAction: () => _run(action));
                  },
            icon: const Icon(Icons.flag),
            label: Text('\u53eb ${_suit(suit)}'),
          ),
        )
        .toList();
  }

  Widget _buildHand(List<CardModel> cards) {
    return SizedBox(
      height: 138,
      child: ListView.separated(
        padding: const EdgeInsets.all(12),
        scrollDirection: Axis.horizontal,
        itemCount: cards.length,
        separatorBuilder: (_, __) => const SizedBox(width: 6),
        itemBuilder: (context, index) {
          final card = cards[index];
          final isSelected = selected.contains(card);
          return GestureDetector(
            onTap: () {
              setState(() {
                isSelected ? selected.remove(card) : selected.add(card);
              });
            },
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 120),
              transform: Matrix4.translationValues(0, isSelected ? -12 : 0, 0),
              width: 58,
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(6),
                border: Border.all(
                  color:
                      isSelected ? Colors.greenAccent.shade700 : Colors.black26,
                  width: 2,
                ),
                boxShadow: const [
                  BoxShadow(
                      blurRadius: 4,
                      offset: Offset(0, 2),
                      color: Colors.black26),
                ],
              ),
              child: Center(
                child: Text(
                  '${_suit(card.suit)}\n${_rank(card.rank)}',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: _isRed(card.suit) ? Colors.red : Colors.black,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  bool _isRed(String? suit) => suit == 'HEART' || suit == 'DIAMOND';

  String _seat(String? seat) {
    return switch (seat) {
      'SOUTH' => '\u5357',
      'WEST' => '\u897f',
      'NORTH' => '\u5317',
      'EAST' => '\u4e1c',
      _ => '\u672a\u5b9a',
    };
  }

  String _phase(String phase) {
    return switch (phase) {
      'DECLARE' => '\u53eb\u4e3b',
      'KITTY' => '\u6263\u5e95',
      'PLAY' => '\u51fa\u724c',
      'FINISHED' => '\u7ed3\u675f',
      _ => phase,
    };
  }

  String _team(String? team) {
    return switch (team) {
      'SOUTH_NORTH' => '\u5357\u5317',
      'EAST_WEST' => '\u4e1c\u897f',
      _ => '\u672a\u5b9a',
    };
  }

  String _suit(String? suit) {
    return switch (suit) {
      'SPADE' => '\u9ed1\u6843',
      'HEART' => '\u7ea2\u5fc3',
      'CLUB' => '\u6885\u82b1',
      'DIAMOND' => '\u65b9\u5757',
      _ => '',
    };
  }

  String _rank(String rank) {
    return switch (rank) {
      'THREE' => '3',
      'FOUR' => '4',
      'FIVE' => '5',
      'SIX' => '6',
      'SEVEN' => '7',
      'EIGHT' => '8',
      'NINE' => '9',
      'TEN' => '10',
      'JACK' => 'J',
      'QUEEN' => 'Q',
      'KING' => 'K',
      'ACE' => 'A',
      'TWO' => '2',
      'SMALL_JOKER' => '\u5c0f\u738b',
      'BIG_JOKER' => '\u5927\u738b',
      _ => rank,
    };
  }
}
