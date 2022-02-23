package com.elminster.jcp.ast;

public interface Identifier {

  String getId();

  Identifier EMPTY_IDENTIFIER = Identifier.fromName("");

  static Identifier fromName(String name) {
    return new Identifier() {
      @Override
      public String getId() {
        return name;
      }

      @Override
      public int hashCode() {
        return this.getId().hashCode();
      }

      @Override
      public boolean equals(Object obj) {
        if (obj instanceof Identifier) {
          return this.getId().equals(((Identifier) obj).getId());
        }
        return false;
      }

      @Override
      public String toString() {
        return this.getId();
      }
    };
  }
}
