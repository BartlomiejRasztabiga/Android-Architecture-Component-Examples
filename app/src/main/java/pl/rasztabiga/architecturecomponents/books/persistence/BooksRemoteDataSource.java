package pl.rasztabiga.architecturecomponents.books.persistence;


import android.support.annotation.NonNull;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BooksRemoteDataSource implements BooksDataSource {

    private static BooksRemoteDataSource instance;

    private BooksRestApi restApi;

    public static BooksRemoteDataSource getInstance() {
        if (instance == null) {
            instance = new BooksRemoteDataSource();
        }
        return instance;
    }

    private BooksRemoteDataSource() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://59bd098b5037eb00117b4b17.mockapi.io/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.restApi = retrofit.create(BooksRestApi.class);
    }

    @Override
    public void getBooks(@NonNull LoadBooksCallback callback) {
        Call<List<Book>> booksCall = restApi.getBooks();

        Observable.fromCallable(() -> booksCall.execute().body())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback::onBooksLoaded, e -> callback.onDataNotAvailable());
    }

    @Override
    public void getBook(@NonNull Long bookId, @NonNull GetBookCallback callback) {
        Call<Book> getBookCall = restApi.getBook(bookId);

        Observable.fromCallable(() -> getBookCall.execute().body())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback::onBookLoaded, e -> callback.onDataNotAvailable());
    }

    @Override
    public void saveBook(@NonNull Book book, @NonNull SaveBookCallback callback) {
        Call<Book> createBookCall = restApi.createBook(book);

        Observable.fromCallable(() -> createBookCall.execute().body().getId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback::onBookSaved, e -> callback.onDataNotAvailable());
    }

    @Override
    public void saveBook(@NonNull Book book) {
        saveBook(book, new SaveBookCallback() {});
    }

    @Override
    public void updateBook(@NonNull Book book) {
        Call<Book> updateBookCall = restApi.updateBook(book.getId(), book);

        Observable.fromCallable(() -> updateBookCall.execute().body())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(e -> {/*ignore*/}, f -> {/*ignore*/});
    }

    @Override
    public void completeBook(@NonNull Book book) {
        updateBook(book);
    }

    @Override
    public void completeBook(@NonNull Long bookId) {
        // Not required for the remote data source because the {@link BooksRepository} handles
        // converting from a {@code bookId} to a {@link book} using its cached data.
    }

    @Override
    public void refreshBooks() {
        // Not required because the {@link BooksRepository} handles the logic of refreshing the
        // books from all the available data sources.
    }

    @Override
    public void deleteAllBooks() {
        // Cannot be handled by mockapi.io
    }

    @Override
    public void deleteBook(@NonNull Long bookId) {
        Call<Book> deleteBookCall = restApi.deleteBook(bookId);

        Observable.fromCallable(() -> deleteBookCall.execute().body())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(e -> {/*ignore*/}, f -> {/*ignore*/});
    }
}
