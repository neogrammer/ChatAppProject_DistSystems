
export interface ISearchResultItem {
    displayName: string;
    id: string;
    email: string;
}

export interface ISearchResult {
    results: ISearchResultItem[];
}