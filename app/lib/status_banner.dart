import 'package:flutter/material.dart';

import 'app_error.dart';

class StatusBanner extends StatefulWidget {
  const StatusBanner({super.key, required this.error, this.onRetry});

  final AppError error;
  final VoidCallback? onRetry;

  @override
  State<StatusBanner> createState() => _StatusBannerState();
}

class _StatusBannerState extends State<StatusBanner> {
  bool _visible = true;

  @override
  void didUpdateWidget(StatusBanner oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!identical(widget.error, oldWidget.error)) {
      _visible = true;
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!_visible) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12),
      child: Material(
        color: Colors.red.shade50,
        borderRadius: BorderRadius.circular(8),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          child: Row(
            children: [
              Icon(Icons.error_outline, color: Colors.red.shade700, size: 18),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  widget.error.message,
                  style: TextStyle(color: Colors.red.shade900, fontSize: 13),
                ),
              ),
              if (widget.error.retryable && widget.onRetry != null)
                Padding(
                  padding: const EdgeInsets.only(left: 8),
                  child: TextButton(
                    onPressed: widget.onRetry,
                    style: TextButton.styleFrom(
                      padding: const EdgeInsets.symmetric(horizontal: 10),
                      visualDensity: VisualDensity.compact,
                    ),
                    child: const Text('重试'),
                  ),
                ),
              GestureDetector(
                onTap: () => setState(() => _visible = false),
                child: Padding(
                  padding: const EdgeInsets.only(left: 4),
                  child:
                      Icon(Icons.close, size: 16, color: Colors.red.shade300),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
