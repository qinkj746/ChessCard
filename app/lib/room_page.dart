import 'dart:async';

import 'package:flutter/material.dart';

import 'api_client.dart';
import 'app_error.dart';
import 'auth_controller.dart';
import 'chat_models.dart';
import 'friend_models.dart';
import 'game_page.dart';
import 'models.dart';
import 'room_connection.dart';
import 'status_banner.dart';

class RoomPage extends StatefulWidget {
  const RoomPage({super.key, this.auth, this.api, this.roomConnectionFactory});

  final AuthController? auth;
  final GameApi? api;
  final RoomEventSource Function(String roomId)? roomConnectionFactory;

  @override
  State<RoomPage> createState() => _RoomPageState();
}

class _RoomPageState extends State<RoomPage> {
  late final GameApi api;
  String? playerId;
  String? playerDisplayName;
  RoomStateModel? room;
  AppError? error;
  bool loading = false;
  RoomEventSource? _roomEvents;
  StreamSubscription<RoomEventModel>? _roomEventSubscription;
  String? _connectedRoomId;
  final TextEditingController _chatController = TextEditingController();
  final List<RoomStateModel> _lobbyRooms = [];
  final List<ChatMessageModel> _chatMessages = [];
  final List<FriendshipModel> _friends = [];
  final List<RoomInvitationModel> _pendingInvitations = [];
  final Set<String> _suppressedLobbyRoomIds = {};
  bool _isRoutePopAllowed = false;

  @override
  void initState() {
    super.initState();
    api = widget.auth?.api ?? widget.api ?? ApiClient();
    _initPlayer();
  }

  @override
  void dispose() {
    _roomEventSubscription?.cancel();
    _roomEvents?.disconnect();
    _chatController.dispose();
    super.dispose();
  }

  Future<void> _initPlayer() async {
    setState(() => loading = true);
    try {
      late final String id;
      late final String displayName;
      if (widget.auth != null) {
        id = await widget.auth!.ensurePlayerId();
        displayName = widget.auth!.displayName ?? id;
      } else {
        final profile = await api.createGuestPlayer();
        id = profile.playerId;
        displayName = profile.displayName;
      }
      if (mounted) {
        setState(() {
          playerId = id;
          playerDisplayName = displayName;
        });
      }
      await _loadLobbyRooms();
    } catch (e) {
      if (mounted) setState(() => error = AppError.fromException(e));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> _createRoom() async {
    if (playerId == null) return;
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final nextRoom = await api.createRoom(playerId!);
      if (!mounted) return;
      final applied = _applyRoomSnapshot(nextRoom);
      if (applied) {
        _attachRoomEvents(nextRoom.roomId);
        await _loadRoomCompanionData(nextRoom.roomId);
      }
    } catch (e) {
      if (mounted) setState(() => error = AppError.fromException(e));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> _loadLobbyRooms() async {
    try {
      final rooms = await api.fetchRooms();
      if (mounted) {
        setState(() {
          _lobbyRooms
            ..clear()
            ..addAll(rooms.where(
              (room) => !_suppressedLobbyRoomIds.contains(room.roomId),
            ));
        });
      }
    } catch (e) {
      if (mounted) setState(() => error = AppError.fromException(e));
    }
  }

  String? _firstEmptySeat(RoomStateModel candidate) {
    for (final seat in const ['SOUTH', 'WEST', 'NORTH', 'EAST']) {
      if (!candidate.seats.containsKey(seat)) return seat;
    }
    return null;
  }

  Future<void> _joinRoomFromLobby(RoomStateModel candidate) async {
    final currentPlayerId = playerId;
    final seat = _firstEmptySeat(candidate);
    if (currentPlayerId == null || seat == null) return;
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final nextRoom = await api.joinSeat(
        candidate.roomId,
        seat,
        currentPlayerId,
      );
      if (!mounted) return;
      final applied = _applyRoomSnapshot(nextRoom);
      if (applied) {
        _attachRoomEvents(nextRoom.roomId);
        await _loadRoomCompanionData(nextRoom.roomId);
      }
    } catch (e) {
      if (mounted) setState(() => error = AppError.fromException(e));
      await _loadLobbyRooms();
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> _joinSeat(String seat) async {
    final currentRoom = room;
    if (playerId == null || currentRoom == null || !_isWaiting(currentRoom)) {
      return;
    }
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final nextRoom = await api.joinSeat(currentRoom.roomId, seat, playerId!);
      if (!mounted) return;
      final applied = _applyRoomSnapshot(nextRoom);
      if (applied) {
        _attachRoomEvents(nextRoom.roomId);
        await _loadRoomCompanionData(nextRoom.roomId);
      }
    } catch (e) {
      if (mounted) setState(() => error = AppError.fromException(e));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> _leaveSeat(String seat) async {
    final currentRoom = room;
    if (playerId == null || currentRoom == null || !_isWaiting(currentRoom)) {
      return;
    }
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final nextRoom = await api.leaveSeat(currentRoom.roomId, seat, playerId!);
      if (!_hasHumanSeats(nextRoom)) {
        await _returnToLobby(departedRoomId: currentRoom.roomId);
      } else {
        _applyRoomSnapshot(nextRoom);
      }
    } catch (e) {
      if (mounted) setState(() => error = AppError.fromException(e));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> _addBot(String seat) async {
    final currentRoom = room;
    if (playerId == null || currentRoom == null || !_isWaiting(currentRoom)) {
      return;
    }
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final nextRoom = await api.addBot(currentRoom.roomId, seat, playerId!);
      _applyRoomSnapshot(nextRoom);
    } catch (e) {
      if (mounted) setState(() => error = AppError.fromException(e));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> _removeBot(String seat) async {
    final currentRoom = room;
    if (playerId == null || currentRoom == null || !_isWaiting(currentRoom)) {
      return;
    }
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final nextRoom = await api.removeBot(currentRoom.roomId, seat, playerId!);
      _applyRoomSnapshot(nextRoom);
    } catch (e) {
      if (mounted) setState(() => error = AppError.fromException(e));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> _startGame() async {
    if (playerId == null || room == null || !_isWaiting(room!)) return;
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final currentRoom = room!;
      final game = await api.startGame(currentRoom.roomId, playerId!);
      if (!mounted) return;
      final returnedGameId = await Navigator.of(context).push<String>(
        MaterialPageRoute(
          builder: (_) => GamePage(
            initialGame: game,
            api: api,
            playerId: playerId!,
            roomId: currentRoom.roomId,
          ),
        ),
      );
      if (returnedGameId != null && mounted) {
        await _refreshRoom(currentRoom.roomId);
      }
    } catch (e) {
      if (mounted) setState(() => error = AppError.fromException(e));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  void _attachRoomEvents(String roomId) {
    if (_connectedRoomId == roomId && _roomEvents != null) return;
    _roomEventSubscription?.cancel();
    _roomEvents?.disconnect();

    final source = widget.roomConnectionFactory?.call(roomId) ??
        RoomConnection(baseUrl: _baseUrl(), roomId: roomId);
    _connectedRoomId = roomId;
    _roomEvents = source;
    _roomEventSubscription = source.events.listen(
      _handleRoomEvent,
      onError: (Object e) {
        if (mounted) setState(() => error = AppError.fromException(e));
      },
    );
    source.connect();
  }

  void _addChatMessageIfAbsent(ChatMessageModel message) {
    if (_chatMessages.every(
      (item) => item.messageId != message.messageId,
    )) {
      _chatMessages.add(message);
    }
  }

  Future<void> _handleRoomEvent(RoomEventModel event) async {
    final currentRoom = room;
    if (currentRoom == null || event.roomId != currentRoom.roomId) return;
    final version = event.version;
    if (version != null && version <= currentRoom.version) return;
    if (event.type == 'CHAT_MESSAGE') {
      final payload = event.payload;
      if (payload is Map) {
        final message = ChatMessageModel.fromJson(
          Map<String, dynamic>.from(payload),
        );
        if (mounted) {
          setState(() => _addChatMessageIfAbsent(message));
        }
      }
      return;
    }
    if (event.type == 'ROOM_INVITATION') {
      final payload = event.payload;
      final currentPlayerId = playerId;
      if (payload is Map && currentPlayerId != null) {
        final invitation = RoomInvitationModel.fromJson(
          Map<String, dynamic>.from(payload),
        );
        if (invitation.toPlayerId == currentPlayerId && mounted) {
          setState(() {
            _pendingInvitations.removeWhere(
              (item) => item.invitationId == invitation.invitationId,
            );
            _pendingInvitations.add(invitation);
          });
        }
      }
      return;
    }
    if (event.type == 'ROOM_UPDATED' ||
        event.type == 'PLAYER_JOINED' ||
        event.type == 'PLAYER_LEFT' ||
        event.type == 'GAME_UPDATED') {
      await _refreshRoom(currentRoom.roomId);
    }
  }

  Future<void> _loadRoomCompanionData(String roomId) async {
    await Future.wait([
      _loadChatMessages(roomId),
      _loadFriendsAndInvitations(),
    ]);
  }

  Future<void> _loadFriendsAndInvitations() async {
    final currentPlayerId = playerId;
    if (currentPlayerId == null) return;
    try {
      final results = await Future.wait<dynamic>([
        api.fetchFriends(currentPlayerId),
        api.fetchPendingInvitations(currentPlayerId),
      ]);
      if (mounted) {
        setState(() {
          _friends
            ..clear()
            ..addAll(results[0] as List<FriendshipModel>);
          _pendingInvitations
            ..clear()
            ..addAll(results[1] as List<RoomInvitationModel>);
        });
      }
    } catch (e) {
      if (mounted) setState(() => error = AppError.fromException(e));
    }
  }

  Future<void> _loadChatMessages(String roomId) async {
    try {
      final messages = await api.fetchRoomMessages(roomId);
      if (mounted) {
        setState(() {
          _chatMessages
            ..clear()
            ..addAll(messages);
        });
      }
    } catch (e) {
      if (mounted) setState(() => error = AppError.fromException(e));
    }
  }

  Future<void> _sendChatMessage() async {
    final currentRoom = room;
    final currentPlayerId = playerId;
    final content = _chatController.text.trim();
    if (currentRoom == null || currentPlayerId == null || content.isEmpty) {
      return;
    }
    setState(() => error = null);
    try {
      final message = await api.sendRoomMessage(
        roomId: currentRoom.roomId,
        playerId: currentPlayerId,
        content: content,
      );
      if (!mounted) return;
      setState(() {
        _addChatMessageIfAbsent(message);
        _chatController.clear();
      });
    } catch (e) {
      if (mounted) setState(() => error = AppError.fromException(e));
    }
  }

  Future<void> _inviteFriend(FriendshipModel friendship) async {
    final currentRoom = room;
    final currentPlayerId = playerId;
    if (currentRoom == null || currentPlayerId == null) return;
    setState(() => error = null);
    try {
      await api.createRoomInvitation(
        roomId: currentRoom.roomId,
        fromPlayerId: currentPlayerId,
        toPlayerId: friendship.otherPlayerId(currentPlayerId),
      );
    } catch (e) {
      if (mounted) setState(() => error = AppError.fromException(e));
    }
  }

  Future<void> _respondToInvitation(
    RoomInvitationModel invitation,
    bool accepted,
  ) async {
    final currentPlayerId = playerId;
    if (currentPlayerId == null) return;
    setState(() => error = null);
    try {
      await api.respondToInvitation(
        invitationId: invitation.invitationId,
        playerId: currentPlayerId,
        accepted: accepted,
      );
      if (!mounted) return;
      setState(() {
        _pendingInvitations.removeWhere(
          (item) => item.invitationId == invitation.invitationId,
        );
      });
    } catch (e) {
      if (mounted) setState(() => error = AppError.fromException(e));
    }
  }

  Future<void> _refreshRoom(String roomId) async {
    try {
      final nextRoom = await api.getRoom(roomId);
      _applyRoomSnapshot(nextRoom);
    } catch (e) {
      if (_isRoomNotFound(e)) {
        await _returnToLobby(departedRoomId: roomId);
        return;
      }
      if (mounted) setState(() => error = AppError.fromException(e));
    }
  }

  bool _isRoomNotFound(Object error) {
    return error is GameApiException && error.code == 'ROOM_NOT_FOUND';
  }

  bool _applyRoomSnapshot(RoomStateModel nextRoom) {
    if (!mounted) return false;
    final currentRoom = room;
    if (currentRoom != null) {
      if (currentRoom.roomId != nextRoom.roomId) return false;
      if (nextRoom.version < currentRoom.version) return false;
    }
    setState(() => room = nextRoom);
    return true;
  }

  Future<void> _returnToLobby({String? departedRoomId}) async {
    final subscription = _roomEventSubscription;
    final roomEvents = _roomEvents;
    _roomEventSubscription = null;
    _roomEvents = null;
    _connectedRoomId = null;

    if (!mounted) return;
    if (departedRoomId != null) {
      _suppressedLobbyRoomIds.add(departedRoomId);
    }
    setState(() {
      room = null;
      _chatMessages.clear();
      _pendingInvitations.clear();
    });
    await _loadLobbyRooms();
    unawaited(_disposeRoomEvents(subscription, roomEvents));
  }

  Future<void> _disposeRoomEvents(
    StreamSubscription<RoomEventModel>? subscription,
    RoomEventSource? roomEvents,
  ) async {
    try {
      await subscription?.cancel();
      roomEvents?.disconnect();
    } catch (_) {
      // Leaving the room should not be blocked by transport cleanup.
    }
  }

  bool _hasHumanSeats(RoomStateModel candidate) {
    return candidate.seats.values.any(
      (seat) =>
          !seat.isBot && seat.playerId != null && seat.playerId!.isNotEmpty,
    );
  }

  String? _humanSeatFor(RoomStateModel candidate, String id) {
    for (final entry in candidate.seats.entries) {
      final seat = entry.value;
      if (!seat.isBot && seat.playerId == id) return entry.key;
    }
    return null;
  }

  Future<bool> _leaveRoomBeforeRoutePop() async {
    final currentRoom = room;
    final currentPlayerId = playerId;
    if (currentRoom == null || currentPlayerId == null) return true;
    if (loading) return false;
    final seat = _humanSeatFor(currentRoom, currentPlayerId);
    if (seat == null) return true;

    setState(() {
      loading = true;
      error = null;
    });
    try {
      if (_isWaiting(currentRoom)) {
        await api.leaveSeat(currentRoom.roomId, seat, currentPlayerId);
      } else if (currentRoom.phase == 'PLAYING') {
        await api.leavePlayingRoom(currentRoom.roomId, currentPlayerId);
      }
      return true;
    } catch (e) {
      if (mounted) setState(() => error = AppError.fromException(e));
      return false;
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> _handleRoutePop(bool didPop, Object? result) async {
    if (didPop || _isRoutePopAllowed) return;
    if (!await _leaveRoomBeforeRoutePop() || !mounted) return;
    setState(() => _isRoutePopAllowed = true);
    await WidgetsBinding.instance.endOfFrame;
    if (mounted) Navigator.of(context).pop();
  }

  bool _isWaiting(RoomStateModel room) => room.phase == 'WAITING';

  String _baseUrl() =>
      api is ApiClient ? (api as ApiClient).baseUrl : 'http://localhost:8080';

  @override
  Widget build(BuildContext context) {
    return PopScope<Object?>(
      canPop: room == null || _isRoutePopAllowed,
      onPopInvokedWithResult: _handleRoutePop,
      child: Scaffold(
        appBar: AppBar(title: const Text('\u623f\u95f4')),
        body: _buildBody(),
      ),
    );
  }

  Widget _buildBody() {
    if (loading && room == null) {
      return const Center(child: CircularProgressIndicator());
    }
    if (playerId == null) {
      return const Center(
          child: Text('\u6b63\u5728\u83b7\u53d6\u73a9\u5bb6\u8eab\u4efd...'));
    }
    if (room == null) {
      return _buildLobby();
    }
    return _buildRoom();
  }

  Widget _buildLobby() {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              const Icon(Icons.person, size: 18),
              const SizedBox(width: 6),
              Expanded(
                child: Text(
                  '\u73a9\u5bb6: ${playerDisplayName ?? playerId}',
                  style: Theme.of(context).textTheme.bodyMedium,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              IconButton(
                onPressed: loading ? null : _loadLobbyRooms,
                icon: const Icon(Icons.refresh),
                tooltip: '\u5237\u65b0\u5927\u5385',
              ),
              FilledButton.icon(
                onPressed: loading ? null : _createRoom,
                icon: const Icon(Icons.add),
                label: Text(
                  loading
                      ? '\u521b\u5efa\u4e2d...'
                      : '\u521b\u5efa\u623f\u95f4',
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          if (error != null) ...[
            StatusBanner(error: error!, onRetry: _loadLobbyRooms),
            const SizedBox(height: 12),
          ],
          Expanded(
            child: _lobbyRooms.isEmpty
                ? Center(
                    child: Text(
                      '\u6682\u65e0\u53ef\u52a0\u5165\u623f\u95f4',
                      style: TextStyle(color: Colors.grey.shade600),
                    ),
                  )
                : ListView.separated(
                    itemCount: _lobbyRooms.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 8),
                    itemBuilder: (context, index) {
                      final candidate = _lobbyRooms[index];
                      final seatCount = candidate.seats.length;
                      return Card(
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: ListTile(
                          leading: const Icon(Icons.meeting_room),
                          title: Text(
                            '\u623f\u95f4: ${candidate.roomId}',
                            overflow: TextOverflow.ellipsis,
                          ),
                          subtitle: Text(
                            '\u623f\u4e3b: ${_truncateId(candidate.ownerPlayerId)}  $seatCount/4',
                            overflow: TextOverflow.ellipsis,
                          ),
                          trailing: FilledButton(
                            onPressed: loading
                                ? null
                                : () => _joinRoomFromLobby(candidate),
                            child: const Text('\u52a0\u5165'),
                          ),
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildRoom() {
    final currentRoom = room!;
    final isOwner = currentRoom.ownerPlayerId == playerId;
    final isWaiting = _isWaiting(currentRoom);
    const seats = ['SOUTH', 'WEST', 'NORTH', 'EAST'];
    final seatCount = currentRoom.seats.length;

    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          Row(
            children: [
              const Icon(Icons.person, size: 18),
              const SizedBox(width: 6),
              Text('\u73a9\u5bb6: ${playerDisplayName ?? playerId}',
                  style: Theme.of(context).textTheme.bodyMedium),
              const Spacer(),
              if (isOwner)
                const Chip(
                    label: Text('\u623f\u4e3b'),
                    visualDensity: VisualDensity.compact),
            ],
          ),
          const SizedBox(height: 8),
          Text('\u623f\u95f4: ${currentRoom.roomId}',
              style: Theme.of(context).textTheme.bodySmall),
          const SizedBox(height: 16),
          Expanded(
            child: GridView.count(
              crossAxisCount: 2,
              mainAxisSpacing: 12,
              crossAxisSpacing: 12,
              childAspectRatio: 1.4,
              children:
                  seats.map((seat) => _seatCard(seat, currentRoom)).toList(),
            ),
          ),
          _friendInvitationPanel(),
          const SizedBox(height: 8),
          _chatPanel(),
          Padding(
            padding: const EdgeInsets.only(top: 12),
            child: Column(
              children: [
                if (isOwner)
                  FilledButton.icon(
                    onPressed: loading || seatCount < 4 || !isWaiting
                        ? null
                        : _startGame,
                    icon: const Icon(Icons.play_arrow),
                    label: Text(loading
                        ? '\u5f00\u59cb\u4e2d...'
                        : '\u5f00\u59cb\u6e38\u620f ($seatCount/4 \u5ea7)'),
                  ),
                if (error != null) ...[
                  const SizedBox(height: 8),
                  StatusBanner(error: error!),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _friendInvitationPanel() {
    final currentPlayerId = playerId;
    final acceptedFriends = _friends
        .where((friendship) => friendship.status == 'ACCEPTED')
        .toList();
    final visibleInvitations = _pendingInvitations
        .where((invitation) => invitation.status == 'PENDING')
        .toList();
    if (currentPlayerId == null ||
        (acceptedFriends.isEmpty && visibleInvitations.isEmpty)) {
      return const SizedBox.shrink();
    }
    return SizedBox(
      height: 88,
      child: ListView(
        scrollDirection: Axis.horizontal,
        children: [
          for (final friendship in acceptedFriends)
            Container(
              width: 180,
              margin: const EdgeInsets.only(right: 8),
              padding: const EdgeInsets.symmetric(horizontal: 8),
              decoration: BoxDecoration(
                border: Border.all(color: Colors.grey.shade300),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      _truncateId(friendship.otherPlayerId(currentPlayerId)),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  IconButton(
                    onPressed: () => _inviteFriend(friendship),
                    icon: const Icon(Icons.person_add),
                    tooltip: 'Invite friend',
                  ),
                ],
              ),
            ),
          for (final invitation in visibleInvitations)
            Container(
              width: 220,
              margin: const EdgeInsets.only(right: 8),
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                border: Border.all(color: Colors.grey.shade300),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                children: [
                  const Icon(Icons.mail_outline, size: 18),
                  const SizedBox(width: 6),
                  Expanded(
                    child: Text(
                      invitation.fromPlayerId,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  IconButton(
                    onPressed: () => _respondToInvitation(invitation, true),
                    icon: const Icon(Icons.check),
                    tooltip: 'Accept invitation',
                  ),
                  IconButton(
                    onPressed: () => _respondToInvitation(invitation, false),
                    icon: const Icon(Icons.close),
                    tooltip: 'Decline invitation',
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }

  Widget _chatPanel() {
    return SizedBox(
      height: 156,
      child: Column(
        children: [
          Expanded(
            child: Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
              decoration: BoxDecoration(
                border: Border.all(color: Colors.grey.shade300),
                borderRadius: BorderRadius.circular(8),
              ),
              child: _chatMessages.isEmpty
                  ? Text('暂无消息', style: TextStyle(color: Colors.grey.shade600))
                  : ListView.builder(
                      itemCount: _chatMessages.length,
                      itemBuilder: (context, index) {
                        final message = _chatMessages[index];
                        final isMe = message.senderPlayerId == playerId;
                        return Align(
                          alignment: isMe
                              ? Alignment.centerRight
                              : Alignment.centerLeft,
                          child: Padding(
                            padding: const EdgeInsets.symmetric(vertical: 2),
                            child: Text(message.content),
                          ),
                        );
                      },
                    ),
            ),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _chatController,
                  maxLength: 200,
                  decoration: const InputDecoration(
                    hintText: '输入消息',
                    counterText: '',
                    border: OutlineInputBorder(),
                    isDense: true,
                  ),
                  onSubmitted: (_) => _sendChatMessage(),
                ),
              ),
              const SizedBox(width: 8),
              IconButton.filled(
                onPressed: _sendChatMessage,
                icon: const Icon(Icons.send),
                tooltip: '发送',
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _seatCard(String seat, RoomStateModel currentRoom) {
    final seatInfo = currentRoom.seats[seat];
    final occupied = seatInfo != null;
    final isBot = occupied && seatInfo.isBot;
    final isMe = occupied && !isBot && seatInfo.playerId == playerId;
    final isOwner = currentRoom.ownerPlayerId == playerId;
    final isWaiting = _isWaiting(currentRoom);
    final isSeated = currentRoom.seats.values.any(
      (info) => !info.isBot && info.playerId == playerId,
    );

    return Card(
      elevation: occupied ? 2 : 0,
      color: occupied
          ? (isBot
              ? Colors.amber.shade50
              : (isMe ? Colors.green.shade50 : Colors.blue.shade50))
          : Colors.grey.shade100,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      child: LayoutBuilder(
        builder: (context, constraints) {
          final compactSpacing = constraints.maxWidth < 150;
          return Padding(
            padding: EdgeInsets.all(compactSpacing ? 4 : 8),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  isBot
                      ? Icons.smart_toy
                      : (occupied ? Icons.person : Icons.person_outline),
                  size: 28,
                  color: isBot
                      ? Colors.amber.shade700
                      : (occupied
                          ? (isMe ? Colors.green : Colors.blue)
                          : Colors.grey),
                ),
                SizedBox(height: compactSpacing ? 2 : 4),
                Text(
                  isBot
                      ? '\u4eba\u673a'
                      : (occupied ? seatInfo.displayName : '\u7a7a\u4f4d'),
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: occupied ? FontWeight.w600 : FontWeight.normal,
                    color: occupied ? null : Colors.grey,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
                SizedBox(height: compactSpacing ? 4 : 6),
                SizedBox(
                  height: 28,
                  child: !occupied
                      ? _emptySeatActions(
                          seat,
                          isOwner: isOwner,
                          isSeated: isSeated,
                          isWaiting: isWaiting,
                        )
                      : isBot && isOwner && isWaiting
                          ? OutlinedButton.icon(
                              onPressed:
                                  loading ? null : () => _removeBot(seat),
                              style: _seatActionStyle(),
                              icon: const Icon(Icons.close, size: 16),
                              label: const Text(
                                '\u79fb\u9664',
                                style: TextStyle(fontSize: 12),
                              ),
                            )
                          : isMe && isWaiting
                              ? OutlinedButton(
                                  onPressed:
                                      loading ? null : () => _leaveSeat(seat),
                                  style: _seatActionStyle(),
                                  child: const Text(
                                    '\u79bb\u5f00',
                                    style: TextStyle(fontSize: 12),
                                  ),
                                )
                              : const SizedBox.shrink(),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _emptySeatActions(
    String seat, {
    required bool isOwner,
    required bool isSeated,
    required bool isWaiting,
  }) {
    if (!isWaiting) return const SizedBox.shrink();
    if (isSeated) {
      if (!isOwner) return const SizedBox.shrink();
      return OutlinedButton.icon(
        onPressed: loading ? null : () => _addBot(seat),
        style: _seatActionStyle(),
        icon: const Icon(Icons.smart_toy, size: 16),
        label: const Text(
          '\u6dfb\u52a0\u4eba\u673a',
          style: TextStyle(fontSize: 12),
        ),
      );
    }

    final joinButton = OutlinedButton(
      onPressed: loading ? null : () => _joinSeat(seat),
      style: _seatActionStyle(),
      child: const Text(
        '\u5165\u5ea7',
        style: TextStyle(fontSize: 12),
      ),
    );
    if (!isOwner) return joinButton;

    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Flexible(child: joinButton),
        const SizedBox(width: 4),
        IconButton(
          onPressed: loading ? null : () => _addBot(seat),
          padding: EdgeInsets.zero,
          constraints: const BoxConstraints.tightFor(width: 28, height: 28),
          visualDensity: VisualDensity.compact,
          icon: const Icon(Icons.smart_toy, size: 18),
          tooltip: '\u6dfb\u52a0\u4eba\u673a',
        ),
      ],
    );
  }

  ButtonStyle _seatActionStyle() {
    return OutlinedButton.styleFrom(
      padding: const EdgeInsets.symmetric(horizontal: 8),
      visualDensity: VisualDensity.compact,
    );
  }

  String _truncateId(String id) =>
      id.length > 12 ? '${id.substring(0, 12)}...' : id;
}
