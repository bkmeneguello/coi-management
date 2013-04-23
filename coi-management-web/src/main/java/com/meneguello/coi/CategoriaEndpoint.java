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
	@Produces(MediaType.APPLICATION_JSON)
	public List<Categoria> list() throws Exception {
		return new Transaction<List<Categoria>>() {
			@Override
			protected List<Categoria> execute(Executor database) {
				ArrayList<Categoria> categorias = new ArrayList<Categoria>();
				Result<CategoriaRecord> resultCategoriaRecord = database.fetch(CATEGORIA);
				for (CategoriaRecord categoriaRecord : resultCategoriaRecord) {
					categorias.add(buildCategoria(categoriaRecord));
				}
				return categorias;
			}

		}.execute();
	}
	
	private Categoria buildCategoria(CategoriaRecord categoriaRecord) {
		final Categoria categoria = new Categoria();
		categoria.setId(categoriaRecord.getId());
		categoria.setDescricao(categoriaRecord.getDescricao());
		return categoria;
	}
	
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Categoria read(final @PathParam("id") Long id) throws Exception {
		return new Transaction<Categoria>() {
			@Override
			protected Categoria execute(Executor database) {
				final CategoriaRecord categoriaRecord = database.selectFrom(CATEGORIA)
						.where(CATEGORIA.ID.eq(id))
						.fetchOne();
				final Categoria categoria = buildCategoria(categoriaRecord);
				
				final Result<ProdutoRecord> resultProdutoRecord = database.selectFrom(PRODUTO)
						.where(PRODUTO.CATEGORIA_ID.eq(categoriaRecord.getId()))
						.fetch();
				for (ProdutoRecord produtoRecord : resultProdutoRecord) {
					categoria.getProdutos().add(buildProduto(produtoRecord));
				}
				
				final Result<Record2<String, BigDecimal>> recordsComissao = database.select(
							PARTE.DESCRICAO,
							COMISSAO.PORCENTAGEM
						)
						.from(COMISSAO)
						.join(PARTE).onKey()
						.where(COMISSAO.CATEGORIA_ID.eq(categoriaRecord.getId()))
						.fetch();
				
				for (Record2<String, BigDecimal> recordComissao : recordsComissao) {
					final Comissao comissao = new Comissao();
					comissao.setParte(recordComissao.getValue(PARTE.DESCRICAO));
					comissao.setPorcentagem(recordComissao.getValue(COMISSAO.PORCENTAGEM));
					categoria.getComissoes().add(comissao);
				}
				
				return categoria;
			}

		}.execute();
	}
	
	private Produto buildProduto(ProdutoRecord produtoRecord) {
		final Produto produto = new Produto();
		produto.setCodigo(produtoRecord.getCodigo());
		produto.setDescricao(produtoRecord.getDescricao());
		produto.setCusto(produtoRecord.getCusto());
		produto.setPreco(produtoRecord.getPreco());
		return produto;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Categoria create(final Categoria categoria) throws Exception {
		final Long categoriaId = new Transaction<Long>(true) {
			@Override
			public Long execute(Executor database) {
				final CategoriaRecord categoriaRecord = database.insertInto(
							CATEGORIA, 
							CATEGORIA.DESCRICAO
						)
						.values(categoria.getDescricao())
						.returning(CATEGORIA.ID)
						.fetchOne();
				
				final Long id = categoriaRecord.getId();
				for (Produto produto : categoria.getProdutos()) {
					database.insertInto(PRODUTO, 
								PRODUTO.CATEGORIA_ID, 
								PRODUTO.CODIGO, 
								PRODUTO.DESCRICAO, 
								PRODUTO.CUSTO, 
								PRODUTO.PRECO
							)
							.values(
									id, 
									produto.getCodigo(), 
									produto.getDescricao(), 
									produto.getCusto(), 
									produto.getPreco()
							)
							.execute();
				}
				for (Comissao comissao : categoria.getComissoes()) {
					final ParteRecord parteRecord = database.selectFrom(PARTE)
							.where(PARTE.DESCRICAO.eq(comissao.getParte()))
							.fetchOne();
					
					database.insertInto(COMISSAO, 
								COMISSAO.CATEGORIA_ID, 
								COMISSAO.PARTE_ID, 
								COMISSAO.PORCENTAGEM
							)
							.values(
									id, 
									parteRecord.getId(), 
									comissao.getPorcentagem())
							.execute();
				}
				
				return id;
			}
		}.execute();
		
		return read(categoriaId);
	}
	
	@PUT
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Categoria update(final @PathParam("id") Long id, final Categoria categoria) throws Exception {
		new Transaction<Void>(true) {
			@Override
			public Void execute(Executor database) {
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
								PRODUTO.PRECO
							)
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
								COMISSAO.PORCENTAGEM
							)
							.values(
									id, 
									parteRecord.getId(), 
									comissao.getPorcentagem())
							.execute();
				}
				
				return null;
			}
		}.execute();
		
		return read(id);
	}
	
	@DELETE
	@Path("/{id}")
	public void delete(final @PathParam("id") Long id) throws Exception {
		new Transaction<Void>(true) {
			@Override
			protected Void execute(Executor database) {
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
	
	private Long id;
	
	private String descricao;
	
	private List<Produto> produtos = new ArrayList<>();
	
	private List<Comissao> comissoes = new ArrayList<>();
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
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