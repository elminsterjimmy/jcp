# Struct Type Example

JCP now supports struct (record) types for creating custom data structures with named, typed fields.

## Basic Usage

### Declaring a Struct

```java
struct Point {
    int x;
    int y;
}
```

### Creating an Instance

```java
Point p = Point(10, 20);
```

### Accessing Fields

```java
int xVal = p.x;
int yVal = p.y;
```

### Modifying Fields

```java
p.x = 15;
p.y = 25;
```

## Complete Example

```java
// Define a struct type
struct Point {
    int x;
    int y;
}

// Create an instance
Point p = Point(10, 20);

// Access fields
int xValue = p.x;  // xValue = 10

// Modify fields
p.y = 30;

// Access modified field
int yValue = p.y;  // yValue = 30
```

## Nested Structs

Structs can contain other structs as fields:

```java
struct Point {
    int x;
    int y;
}

struct Rectangle {
    Point topLeft;
    Point bottomRight;
}

// Create nested struct
Point tl = Point(0, 0);
Point br = Point(100, 50);
Rectangle rect = Rectangle(tl, br);

// Access nested fields
int width = rect.bottomRight.x - rect.topLeft.x;
```

## Current Limitations

- **Eval mode only**: Struct types currently work in interpreter mode only. Compile mode (JVM bytecode generation) is planned for a future release.
- **No methods**: Structs cannot have methods, only data fields.
- **No inheritance**: Structs cannot extend other structs.
- **Forward references**: Structs must be declared before use.
- **Constructor args**: All fields must be provided when creating an instance.

## Type System

Structs are first-class types in the JCP type system:
- They inherit from `ANY` in the type hierarchy
- Field values are type-checked on instantiation and assignment
- Struct instances can be stored in variables and passed as values
