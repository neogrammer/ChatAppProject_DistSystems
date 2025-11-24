export interface IElementConvertable {
    asElement<T extends Element = Element>(): T;
}