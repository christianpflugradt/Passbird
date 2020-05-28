package de.pflugradts.pwman3.domain.model.transfer;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class InputFaker {

    private String message;

    public static InputFaker faker() {
        return new InputFaker();
    }

    public InputFaker fakeInput() {
        this.message = "hi";
        return this;
    }

    public InputFaker withMessage(final String message) {
        this.message = message;
        return this;
    }

    public Input fake() {
        return Input.of(Bytes.of(message));
    }

}
