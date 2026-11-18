package wampus;

import java.util.ArrayList;
import java.util.List;

record Percept(boolean stench, boolean breeze, boolean glitter, boolean bump, boolean scream) {
    static final Percept QUIET = new Percept(false, false, false, false, false);
    static final List<String> NAMES = List.of("Stench", "Breeze", "Glitter", "Bump", "Scream");

    List<Boolean> signals() {
        return List.of(stench, breeze, glitter, bump, scream);
    }

    String tuple() {
        List<String> fields = new ArrayList<>(NAMES.size());
        List<Boolean> active = signals();
        for (int i = 0; i < NAMES.size(); i++) {
            fields.add(active.get(i) ? NAMES.get(i) : "none");
        }
        return "(" + String.join(", ", fields) + ")";
    }
}
