package pl.rasztabiga.architecturecomponents.books.persistence;


import android.support.annotation.NonNull;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.google.common.base.Preconditions.checkNotNull;

public class BooksLocalDataSource implements BooksDataSource {

    private static BooksLocalDataSource instance;

    private BooksDao mBooksDao;

    private BooksLocalDataSource(@NonNull BooksDao booksDao) {
        mBooksDao = booksDao;
    }

    public static BooksLocalDataSource getInstance(@NonNull BooksDao booksDao) {
        if (instance == null) {
            instance = new BooksLocalDataSource(booksDao);
        }
        return instance;
    }

    @Override
    public void getBooks(@NonNull LoadBooksCallback callback) {
        Observable.fromCallable(() -> mBooksDao.getBooks())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(books -> {
                    if (books.isEmpty()) {
                        callback.onDataNotAvailable();
                    } else {
                        callback.onBooksLoaded(books);
                    }
                });
    }

    @Override
    public void getBook(@NonNull Long bookId, @NonNull GetBookCallback callback) {
        Observable.fromCallable(() -> mBooksDao.getBookById(bookId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(book -> {
                    if (book == null) {
                        callback.onDataNotAvailable();
                    } else {
                        callback.onBookLoaded(book);
                    }
                });
    }

    @Override
    public void saveBook(@NonNull Book book, @NonNull SaveBookCallback callback) {
        checkNotNull(book);
        Observable.fromCallable(() -> mBooksDao.insertBook(book))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback::onBookSaved, f -> callback.onDataNotAvailable());
    }

    @Override
    public void updateBook(@NonNull Book book) {
        checkNotNull(book);
        Observable.fromCallable(() -> {
            mBooksDao.updateBook(book);
            return Observable.empty();
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    @Override
    public void completeBook(@NonNull Book book) {
        checkNotNull(book);
        Observable.fromCallable(() -> {
            mBooksDao.updateBookWithCompleted(book.getId(), book.isCompleted());
            return Observable.empty();
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    @Override
    public void completeBook(@NonNull Long bookId) {
        // Not required for the local data source because the {@link BooksRepository} handles
        // converting from a {@code bookId} to a {@link book} using its cached data.
    }

    @Override
    public void refreshBooks() {
        // Not required because the {@link BooksRepository} handles the logic of refreshing the
        // books from all the available data sources.
    }

    @Override
    public void deleteAllBooks() {
        Observable.fromCallable(() -> {
            mBooksDao.deleteBooks();
            return Observable.empty();
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    @Override
    public void deleteBook(@NonNull Long bookId) {
        Observable.fromCallable(() -> {
            mBooksDao.deleteBookById(bookId);
            return Observable.empty();
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }
}
