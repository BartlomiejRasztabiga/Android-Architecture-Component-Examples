package pl.rasztabiga.architecturecomponents;

import android.content.Context;
import android.support.annotation.NonNull;

import pl.rasztabiga.architecturecomponents.books.BooksRepository;
import pl.rasztabiga.architecturecomponents.books.persistence.BooksDatabase;
import pl.rasztabiga.architecturecomponents.books.persistence.BooksLocalDataSource;
import pl.rasztabiga.architecturecomponents.books.persistence.BooksRemoteDataSource;

import static com.google.common.base.Preconditions.checkNotNull;

public class Injection {

    private Injection() {

    }

    public static BooksRepository provideBooksRepository(@NonNull Context context) {
        checkNotNull(context);
        BooksDatabase database = BooksDatabase.getInstance(context);
        return BooksRepository.getInstance(BooksRemoteDataSource.getInstance(),
                BooksLocalDataSource.getInstance(database.booksDao()));
    }
}