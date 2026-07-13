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
  RoomStateModel? room;
  AppError? error;
  bool loading = false;
  RoomEventSource? _roomEvents;
  StreamSubscription<RoomEventModel>? _roomEventSubscription;
  String? _connectedRoomId;
  final TextEditingController _chatController = TextEditingController();
  final List<ChatMessageModel> _chatMessages = [];
  final List<FriendshipModel> _friends = [];
  final List<RoomInvitationModel> _pendingInvitations = [];

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
      final id = widget.auth != null
          ? await widget.auth!.ensurePlayerId()
          : (await api.createGuestPlayer()).playerId;
      if (mounted) setState(() => playerId = id);
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
      setState(() => room = nextRoom);
      _attachRoomEvents(nextRoom.roomId);
      await _loadRoomCompanionData(nextRoom.roomId);
    } catch (e) {
      if (mounted) setState(() => error = AppError.fromException(e));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> _joinSeat(String seat) async {
    if (playerId == null || room == null) return;
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final nextRoom = await api.joinSeat(room!.roomId, seat, playerId!);
      if (!mounted) return;
      setState(() => room = nextRoom);
      _attachRoomEvents(nextRoom.roomId);
      await _loadRoomCompanionData(nextRoom.roomId);
    } catch (e) {
      if (mounted) setState(() => error = AppError.fromException(e));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> _leaveSeat(String seat) async {
    if (playerId == null || room == null) return;
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final nextRoom = await api.leaveSeat(room!.roomId, seat, playerId!);
      if (mounted) setState(() => room = nextRoom);
    } catch (e) {
      if (mounted) setState(() => error = AppError.fromException(e));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> _startGame() async {
    if (playerId == null || room == null) return;
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
          setState(() {
            if (_chatMessages
                .every((item) => item.messageId != message.messageId)) {
              _chatMessages.add(message);
            }
          });
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
        _chatMessages.add(message);
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
      if (mounted) setState(() => room = nextRoom);
    } catch (e) {
      if (mounted) setState(() => error = AppError.fromException(e));
    }
  }

  String _baseUrl() =>
      api is ApiClient ? (api as ApiClient).baseUrl : 'http://localhost:8080';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('\u623f\u95f4')),
      body: _buildBody(),
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
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text('\u73a9\u5bb6: $playerId',
                style: Theme.of(context).textTheme.bodySmall),
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: loading ? null : _createRoom,
              icon: const Icon(Icons.add),
              label: Text(loading
                  ? '\u521b\u5efa\u4e2d...'
                  : '\u521b\u5efa\u623f\u95f4'),
            ),
            if (error != null) ...[
              const SizedBox(height: 12),
              StatusBanner(error: error!),
            ],
          ],
        ),
      );
    }
    return _buildRoom();
  }

  Widget _buildRoom() {
    final currentRoom = room!;
    final isOwner = currentRoom.ownerPlayerId == playerId;
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
              Text('\u73a9\u5bb6: $playerId',
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
                    onPressed: loading ? null : _startGame,
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
    final isMe = occupied && seatInfo.playerId == playerId;

    return Card(
      elevation: occupied ? 2 : 0,
      color: occupied
          ? (isMe ? Colors.green.shade50 : Colors.blue.shade50)
          : Colors.grey.shade100,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              occupied ? Icons.person : Icons.person_outline,
              size: 28,
              color:
                  occupied ? (isMe ? Colors.green : Colors.blue) : Colors.grey,
            ),
            const SizedBox(height: 6),
            Text(
              occupied ? _truncateId(seatInfo.playerId) : '\u7a7a\u4f4d',
              style: TextStyle(
                fontSize: 12,
                fontWeight: occupied ? FontWeight.w600 : FontWeight.normal,
                color: occupied ? null : Colors.grey,
              ),
              overflow: TextOverflow.ellipsis,
            ),
            const SizedBox(height: 8),
            if (!occupied)
              SizedBox(
                height: 28,
                child: OutlinedButton(
                  onPressed: loading ? null : () => _joinSeat(seat),
                  style: OutlinedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(horizontal: 12),
                    visualDensity: VisualDensity.compact,
                  ),
                  child: const Text('\u5165\u5ea7',
                      style: TextStyle(fontSize: 12)),
                ),
              )
            else if (isMe)
              SizedBox(
                height: 28,
                child: OutlinedButton(
                  onPressed: loading ? null : () => _leaveSeat(seat),
                  style: OutlinedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(horizontal: 12),
                    visualDensity: VisualDensity.compact,
                  ),
                  child: const Text('\u79bb\u5f00',
                      style: TextStyle(fontSize: 12)),
                ),
              ),
          ],
        ),
      ),
    );
  }

  String _truncateId(String id) =>
      id.length > 12 ? '${id.substring(0, 12)}...' : id;
}
