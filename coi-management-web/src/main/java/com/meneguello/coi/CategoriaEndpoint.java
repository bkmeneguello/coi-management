package com.meneguello.coi;

import static com.meneguello.coi.model.tables.Categoria.CATEGORIA;
import static com.meneguello.coi.model.tables.Comissao.COMISSAO;
import static com.meneguello.coi.model.tables.Parte.PARTE;
import static com.meneguello.coi.model.tables.Produto.PRODUTO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.impl.Executor;
import com.meneguello.coi.model.tables.records.CategoriaRecord;
import com.meneguello.coi.model.tables.records.ParteRecord;
import com.meneguello.coi.model.tables.records.ProdutoRecord;
 
@Path("/categorias")
public class CategoriaEndpoint {
	
	@GET
	@Path("/new")
	@Produces(MediaType.APPLICATION_JSON)
	public Categoria createNew() throws Exception {
		return new Transaction<Categoria>() {
			@Override
			protected Categoria execute(Executor database) {
				final Categoria categoria = new Categoria();
				Result<ParteRecord> partesRecord = database.fetch(PARTE);
				for (ParteRecord parteRecord : partesRecord) {
					Comissao comissao = new Comissao();
					comissao.setParte(parteRecord.getDescricao());
					categoria.getComissoes().add(comissao);
				}
				return categoria;
			}
		}.execute();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Categoria> list() throws Exception {
		return new Transaction<List<Categoria>>() {
			@Override
			protected List<Categoria> execute(Executor database) {
				ArrayList<Categoria> categorias = new ArrayList<Categoria>();
				Result<CategoriaRecord> resultCategoriaRecord = database.fetch(CATEGORIA);
				for (CategoriaRecord categoriaRecord : resultCategoriaRecord) {
					Categoria categoria = new Categoria();
					categoria.setDescricao(categoriaRecord.getDescricao());
					categorias.add(categoria);
				}
				return categorias;
			}
		}.execute();
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void create(final Categoria categoria) throws Exception {
		new Transaction<Void>(true) {
			@Override
			public Void execute(Executor database) {
				final CategoriaRecord categoriaRecord = database.insertInto(
						CATEGORIA, 
						CATEGORIA.DESCRICAO)
					.values(categoria.getDescricao())
					.returning(CATEGORIA.ID)
					.fetchOne();
				
				final Long categoriaId = categoriaRecord.getId();
				for (Produto produto : categoria.getProdutos()) {
					database.insertInto(PRODUTO, 
							PRODUTO.CATEGORIA_ID, 
							PRODUTO.CODIGO, 
							PRODUTO.DESCRICAO, 
							PRODUTO.CUSTO, 
							PRODUTO.PRECO)
						.values(
								categoriaId, 
								produto.getCodigo(), 
								produto.getDescricao(), 
								produto.getCusto(), 
								produto.getPreco())
						.execute();
				}
				for (Comissao comissao : categoria.getComissoes()) {
					final ParteRecord parteRecord = database.selectFrom(PARTE)
							.where(PARTE.DESCRICAO.eq(comissao.getParte()))
							.fetchOne();
					
					database.insertInto(COMISSAO, 
							COMISSAO.CATEGORIA_ID, 
							COMISSAO.PARTE_ID, 
							COMISSAO.PORCENTAGEM)
						.values(
								categoriaId, 
								parteRecord.getId(), 
								comissao.getPorcentagem())
						.execute();
				}
				return null;
			}
		}.execute();
	}
	
	@PUT
	@Path("/{descricao}")
	@Consumes(MediaType.APPLICATION_JSON)
	public void update(final @PathParam("descricao") String descricao, final Categoria categoria) throws Exception {
		new Transaction<Void>(true) {
			@Override
			public Void execute(Executor database) {
				final CategoriaRecord categoriaRecord = database.selectFrom(CATEGORIA)
						.where(CATEGORIA.DESCRICAO.eq(descricao))
						.fetchOne();
				
				final Long id = categoriaRecord.getId();
				
				database.update(CATEGORIA)
					.set(CATEGORIA.DESCRICAO, categoria.getDescricao())
					.where(CATEGORIA.ID.eq(id))
					.execute();

				database.delete(PRODUTO)
					.where(PRODUTO.CATEGORIA_ID.eq(id))
					.execute();
				
				for (Produto produto : categoria.getProdutos()) {
					database.insertInto(PRODUTO, 
							PRODUTO.CATEGORIA_ID, 
							PRODUTO.CODIGO, 
							PRODUTO.DESCRICAO, 
							PRODUTO.CUSTO, 
							PRODUTO.PRECO)
						.values(
								id, 
								produto.getCodigo(), 
								produto.getDescricao(), 
								produto.getCusto(), 
								produto.getPreco())
						.execute();
				}
				
				database.delete(COMISSAO)
					.where(COMISSAO.CATEGORIA_ID.eq(id))
					.execute();
				
				for (Comissao comissao : categoria.getComissoes()) {
					final ParteRecord parteRecord = database.selectFrom(PARTE)
							.where(PARTE.DESCRICAO.eq(comissao.getParte()))
							.fetchOne();
					
					database.insertInto(COMISSAO, 
							COMISSAO.CATEGORIA_ID, 
							COMISSAO.PARTE_ID, 
							COMISSAO.PORCENTAGEM)
						.values(
								id, 
								parteRecord.getId(), 
								comissao.getPorcentagem())
						.execute();
				}
				return null;
			}
		}.execute();
	}
 
	@GET
	@Path("/{descricao}")
	@Produces(MediaType.APPLICATION_JSON)
	public Categoria read(final @PathParam("descricao") String descricao) throws Exception {
		return new Transaction<Categoria>() {
			@Override
			protected Categoria execute(Executor database) {
				final CategoriaRecord categoriaRecord = database.selectFrom(CATEGORIA)
						.where(CATEGORIA.DESCRICAO.eq(descricao))
						.fetchOne();
				final Categoria categoria = new Categoria();
				categoria.setDescricao(categoriaRecord.getDescricao());
				
				final Result<ProdutoRecord> resultProdutoRecord = database.selectFrom(PRODUTO)
					.where(PRODUTO.CATEGORIA_ID.eq(categoriaRecord.getId()))
					.fetch();
				for (ProdutoRecord produtoRecord : resultProdutoRecord) {
					final Produto produto = new Produto();
					produto.setCodigo(produtoRecord.getCodigo());
					produto.setDescricao(produtoRecord.getDescricao());
					produto.setCusto(produtoRecord.getCusto());
					produto.setPreco(produtoRecord.getPreco());
					categoria.getProdutos().add(produto);
				}
				
				final Result<Record2<String, BigDecimal>> result = database.select(
						PARTE.DESCRICAO,
						COMISSAO.PORCENTAGEM)
					.from(COMISSAO)
					.join(PARTE).onKey()
					.where(COMISSAO.CATEGORIA_ID.eq(categoriaRecord.getId()))
					.fetch();
				for (Record2<String, BigDecimal> record : result) {
					final Comissao comissao = new Comissao();
					comissao.setParte(record.getValue(PARTE.DESCRICAO));
					comissao.setPorcentagem(record.getValue(COMISSAO.PORCENTAGEM));
					categoria.getComissoes().add(comissao);
				}
				
				return categoria;
			}
		}.execute();
	}
	
	@DELETE
	@Path("/{descricao}")
	public void delete(final @PathParam("descricao") String descricao) throws Exception {
		new Transaction<Void>(true) {
			@Override
			protected Void execute(Executor database) {
				final CategoriaRecord categoriaRecord = database.selectFrom(CATEGORIA)
						.where(CATEGORIA.DESCRICAO.eq(descricao))
						.fetchOne();
				
				final Long id = categoriaRecord.getId();
				
				database.delete(PRODUTO)
					.where(PRODUTO.CATEGORIA_ID.eq(id))
					.execute();
			
				database.delete(COMISSAO)
					.where(COMISSAO.CATEGORIA_ID.eq(id))
					.execute();
				
				database.delete(CATEGORIA)
					.where(CATEGORIA.ID.eq(id))
					.execute();
				
				return null;
			}
		}.execute();
	}

}

class Categoria {
	
	private String descricao;
	
	private List<Produto> produtos = new ArrayList<>();
	
	private List<Comissao> comissoes = new ArrayList<>();
	
	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public List<Produto> getProdutos() {
		return produtos;
	}

	public List<Comissao> getComissoes() {
		return comissoes;
	}
	
}

class Produto {
	
	private String codigo;
	
	private String descricao;
	
	private BigDecimal custo = BigDecimal.ZERO;
	
	private BigDecimal preco = BigDecimal.ZERO;

	public String getCodigo() {
		return codigo;
	}
	
	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}
	
	public String getDescricao() {
		return descricao;
	}
	
	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}
	
	public BigDecimal getCusto() {
		return custo;
	}
	
	public void setCusto(BigDecimal custo) {
		this.custo = custo;
	}
	
	public BigDecimal getPreco() {
		return preco;
	}
	
	public void setPreco(BigDecimal preco) {
		this.preco = preco;
	}
	
}

class Comissao {
	
	private String parte;
	
	private BigDecimal porcentagem = BigDecimal.ZERO;

	public String getParte() {
		return parte;
	}

	public void setParte(String parte) {
		this.parte = parte;
	}

	public BigDecimal getPorcentagem() {
		return porcentagem;
	}

	public void setPorcentagem(BigDecimal porcentagem) {
		this.porcentagem = porcentagem;
	}
	
}