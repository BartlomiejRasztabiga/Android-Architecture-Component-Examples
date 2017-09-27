package pl.rasztabiga.architecturecomponents.books;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pl.rasztabiga.architecturecomponents.books.persistence.Book;
import pl.rasztabiga.architecturecomponents.books.persistence.BooksDataSource;
import pl.rasztabiga.architecturecomponents.util.EspressoIdlingResource;

import static com.google.common.base.Preconditions.checkNotNull;

// TODO Implement this class using LiveData
public class BooksRepository implements BooksDataSource {

    private static volatile BooksRepository instance = null;

    private final BooksDataSource mBooksRemoteDataSource;

    private final BooksDataSource mBooksLocalDataSource;

    Map<Long, Book> mCachedBooks;

    private boolean mCacheIsDirty = false;

    // Prevent direct instantiation.
    private BooksRepository(@NonNull BooksDataSource booksRemoteDataSource,
                            @NonNull BooksDataSource booksLocalDataSource) {
        mBooksRemoteDataSource = checkNotNull(booksRemoteDataSource);
        mBooksLocalDataSource = checkNotNull(booksLocalDataSource);
    }

    public static BooksRepository getInstance(BooksDataSource booksRemoteDataSource,
                                              BooksDataSource booksLocalDataSource) {
        if (instance == null) {
            synchronized (BooksRepository.class) {
                if (instance == null) {
                    instance = new BooksRepository(booksRemoteDataSource, booksLocalDataSource);
                }
            }
        }
        return instance;
    }

    public static void destroyInstance() {
        instance = null;
    }

    @Override
    public void getBooks(@NonNull LoadBooksCallback callback) {
        checkNotNull(callback);

        // Respond immediately with cache if available and not dirty
        if (mCachedBooks != null && !mCacheIsDirty) {
            callback.onBooksLoaded(new ArrayList<>(mCachedBooks.values()));
            return;
        }

        EspressoIdlingResource.increment(); // App is busy until further notice

        if (mCacheIsDirty) {
            // If the cache is dirty we need to fetch new data from the network.
            getBooksFromRemoteDataSource(callback);
        } else {
            // Query the local storage if available. If not, query the network.
            mBooksLocalDataSource.getBooks(new LoadBooksCallback() {
                @Override
                public void onBooksLoaded(List<Book> books) {
                    refreshCache(books);

                    EspressoIdlingResource.decrement(); // Set app as idle.
                    callback.onBooksLoaded(new ArrayList<>(mCachedBooks.values()));
                }

                @Override
                public void onDataNotAvailable() {
                    getBooksFromRemoteDataSource(callback);
                }
            });
        }
    }

    @Override
    public void saveBook(@NonNull Book book, @NonNull SaveBookCallback callback) {
        checkNotNull(book);
        EspressoIdlingResource.increment(); // App is busy until further notice

        // TODO Synchronize local with remote
        mBooksLocalDataSource.saveBook(book, new SaveBookCallback() {
            @Override
            public void onBookSaved(Long bookId) {
                book.setId(bookId); //Update entity with newly generated ID
                Log.d("BooksRepository", "Saved book to local " + book.toString());

                // Do in memory cache update to keep the app UI up to date
                if (mCachedBooks == null) {
                    mCachedBooks = new LinkedHashMap<>();
                }
                mCachedBooks.put(book.getId(), book);

                callback.onBookSaved(bookId);
            }

            @Override
            public void onDataNotAvailable() {
                callback.onDataNotAvailable(); // Local is not available, something is wrong
            }
        });
        mBooksRemoteDataSource.saveBook(book, new SaveBookCallback() {
            @Override
            public void onBookSaved(Long bookId) {
                Log.d("BooksRepository", "Saved book to remote " + book.toString());
                //callback.onBookSaved(book);
            }

            @Override
            public void onDataNotAvailable() {
                Log.d("BooksRepository", "Remote: onDataNotAvailable()");
                callback.onDataNotAvailable();
            }
        });

    }

    @Override
    public void updateBook(@NonNull Book book) {
        checkNotNull(book);
        mBooksRemoteDataSource.updateBook(book);
        mBooksLocalDataSource.updateBook(book);

        // Do in memory cache update to keep the app UI up to date
        if (mCachedBooks == null) {
            mCachedBooks = new LinkedHashMap<>();
        }
        mCachedBooks.put(book.getId(), book);
    }

    @Override
    public void completeBook(@NonNull Book book) {
        checkNotNull(book);
        mBooksRemoteDataSource.completeBook(book);
        mBooksLocalDataSource.completeBook(book);

        Book completedBook = new Book(book.getId(), book.getTitle(), book.getPages(), true);

        // Do in memory cache update to keep the app UI up to date
        if (mCachedBooks == null) {
            mCachedBooks = new LinkedHashMap<>();
        }
        mCachedBooks.put(book.getId(), completedBook);
    }

    @Override
    public void completeBook(@NonNull Long bookId) {
        checkNotNull(bookId);
        Book bookToComplete = checkNotNull(getBookWithId(bookId));
        completeBook(bookToComplete);
    }

    @Override
    public void getBook(@NonNull Long bookId, @NonNull GetBookCallback callback) {
        checkNotNull(bookId);
        checkNotNull(callback);

        Book cachedBook = getBookWithId(bookId);

        // Respond immediately with cache if available
        if (cachedBook != null) {
            callback.onBookLoaded(cachedBook);
            return;
        }

        EspressoIdlingResource.increment(); // App is busy until further notice

        // Load from server/persisted if needed.

        // Is the task in the local data source? If not, query the network.
        mBooksLocalDataSource.getBook(bookId, new GetBookCallback() {
            @Override
            public void onBookLoaded(Book book) {
                // Do in memory cache update to keep the app UI up to date
                if (mCachedBooks == null) {
                    mCachedBooks = new LinkedHashMap<>();
                }
                mCachedBooks.put(book.getId(), book);

                EspressoIdlingResource.decrement(); // Set app as idle.

                callback.onBookLoaded(book);
            }

            @Override
            public void onDataNotAvailable() {
                mBooksRemoteDataSource.getBook(bookId, new GetBookCallback() {
                    @Override
                    public void onBookLoaded(Book book) {
                        if (book == null) {
                            onDataNotAvailable();
                            return;
                        }
                        // Do in memory cache update to keep the app UI up to date
                        if (mCachedBooks == null) {
                            mCachedBooks = new LinkedHashMap<>();
                        }
                        mCachedBooks.put(book.getId(), book);
                        EspressoIdlingResource.decrement(); // Set app as idle.

                        callback.onBookLoaded(book);
                    }

                    @Override
                    public void onDataNotAvailable() {
                        EspressoIdlingResource.decrement(); // Set app as idle.

                        callback.onDataNotAvailable();
                    }
                });
            }
        });
    }

    @Override
    public void refreshBooks() {
        mCacheIsDirty = true;
    }

    @Override
    public void deleteAllBooks() {
        mBooksRemoteDataSource.deleteAllBooks();
        mBooksLocalDataSource.deleteAllBooks();

        if (mCachedBooks == null) {
            mCachedBooks = new LinkedHashMap<>();
        }
        mCachedBooks.clear();
    }

    @Override
    public void deleteBook(@NonNull Long bookId) {
        mBooksRemoteDataSource.deleteBook(checkNotNull(bookId));
        mBooksLocalDataSource.deleteBook(checkNotNull(bookId));

        mCachedBooks.remove(bookId);
    }

    private void getBooksFromRemoteDataSource(@NonNull final LoadBooksCallback callback) {
        mBooksRemoteDataSource.getBooks(new LoadBooksCallback() {
            @Override
            public void onBooksLoaded(List<Book> books) {
                refreshCache(books);
                refreshLocalDataSource(books);

                EspressoIdlingResource.decrement(); // Set app as idle.
                callback.onBooksLoaded(new ArrayList<>(mCachedBooks.values()));
            }

            @Override
            public void onDataNotAvailable() {
                EspressoIdlingResource.decrement(); // Set app as idle
                callback.onDataNotAvailable();
            }
        });
    }

    private void refreshCache(List<Book> books) {
        if (mCachedBooks == null) {
            mCachedBooks = new LinkedHashMap<>();
        }
        mCachedBooks.clear();
        for (Book book : books) {
            mCachedBooks.put(book.getId(), book);
        }
        mCacheIsDirty = false;
    }

    // TODO Add refreshRemoteDataSource
    private void refreshLocalDataSource(List<Book> books) {
        mBooksLocalDataSource.deleteAllBooks();
        for (Book book : books) {
            mBooksLocalDataSource.saveBook(book, new SaveBookCallback() {
                @Override
                public void onBookSaved(Long bookId) {

                }

                @Override
                public void onDataNotAvailable() {

                }

            });
        }
    }

    @Nullable
    private Book getBookWithId(@NonNull Long id) {
        checkNotNull(id);
        if (mCachedBooks == null || mCachedBooks.isEmpty()) {
            return null;
        } else {
            return mCachedBooks.get(id);
        }
    }
}
