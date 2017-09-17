package pl.rasztabiga.architecturecomponents.books.persistence;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface BooksRestApi {

    @GET("books/")
    Call<List<Book>> getBooks();

    @POST("books/")
    Call<Book> createBook(@Body Book book);

    @PUT("books/{id}")
    Call<Book> updateBook(@Path("id") Long bookId, @Body Book book);

    @DELETE("books/{id}")
    Call<Book> deleteBook(@Path("id") Long bookId);
}
