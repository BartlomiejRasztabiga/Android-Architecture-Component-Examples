package pl.rasztabiga.architecturecomponents.books.persistence;

import android.support.annotation.NonNull;

import java.util.List;

public interface BooksDataSource {

    interface LoadBooksCallback {

        void onBooksLoaded(List<Book> books);

        void onDataNotAvailable();
    }

    interface GetBookCallback {

        void onBookLoaded(Book book);

        void onDataNotAvailable();
    }

    interface SaveBookCallback {

        default void onBookSaved(Long bookId) {}

        default void onDataNotAvailable() {}
    }

    void getBooks(@NonNull LoadBooksCallback callback);

    void getBook(@NonNull Long bookId, @NonNull GetBookCallback callback);

    void saveBook(@NonNull Book book, @NonNull SaveBookCallback callback);

    void saveBook(@NonNull Book book);

    void updateBook(@NonNull Book book);

    void completeBook(@NonNull Book book);

    void completeBook(@NonNull Long bookId);

    void refreshBooks();

    void deleteAllBooks();

    void deleteBook(@NonNull Long bookId);
}
