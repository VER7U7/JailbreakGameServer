package com.VER7U7.Server.Gameplay.Rules;

public class BasicRules {
    public enum Team {
        Spectator(0),
        Prisoner(1),
        Escaped(2),
        Police(3);

        private int value;

        Team(int value) {
            this.value = value;
        }

        public short getID() { return (short) this.value; }
        public static Team fromID(int value) {
            for (Team i : Team.values()) {
                if (i.value == value)
                    return i;
            }
            throw new IllegalArgumentException("No enum constant with value " + value);
        }
    }
}
