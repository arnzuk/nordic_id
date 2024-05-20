import 'dart:convert';

class ButtonEvent {
  final int source;
  final int direction;

  ButtonEvent({
    required this.direction,
    required this.source,
  });

  factory ButtonEvent.fromMap(Map<String, dynamic> json) => ButtonEvent(
        source: int.parse(json["source"]),
        direction: int.parse(json["direction"]),
      );

  Map<String, dynamic> toMap() => {
        "source": source,
        "direction": direction,
      };

  static ButtonEvent parseButtonEvent(String str) => ButtonEvent.fromMap(json.decode(str));
  static String buttonEventToJson(ButtonEvent event) => json.encode(event.toMap());
}
