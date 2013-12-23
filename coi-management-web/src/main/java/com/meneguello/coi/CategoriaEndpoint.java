package com.meneguello.coi;

import static com.meneguello.coi.model.tables.Categoria.CATEGORIA;
import static com.meneguello.coi.model.tables.Comissao.COMISSAO;
import static com.meneguello.coi.model.tables.Produto.PRODUTO;
import static org.apache.commons.lang3.StringUtils.trimToNull;

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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import lombok.Data;

import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.jooq.DeleteConditionStep;
import org.jooq.Result;
import org.jooq.SelectWhereStep;

import com.meneguello.coi.model.tables.records.CategoriaRecord;
import com.meneguello.coi.model.tables.records.ComissaoRecord;
import com.meneguello.coi.model.tables.records.ProdutoRecord;
 
@Path("/categorias")
public class CategoriaEndpoint {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Categoria> list() throws Exception {
		return new Transaction<List<Categoria>>() {
			@Override
			protected List<Categoria> execute(DSLContext database) {
				ArrayList<Categoria> categorias = new ArrayList<Categoria>();
				Result<CategoriaRecord> resultCategoriaRecord = database.fetch(CATEGORIA);
				for (CategoriaRecord categoriaRecord : resultCategoriaRecord) {
					categorias.add(buildCategoria(categoriaRecord));
				}
				return categorias;
			}

		}.execute();
	}
	
	@GET
	@Path("/produtos")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Produto> listProdutos(final @QueryParam("term") String term) throws Exception {
		return new Transaction<List<Produto>>() {
			@Override
			protected List<Produto> execute(DSLContext database) {
				final ArrayList<Produto> produtos = new ArrayList<Produto>();
				final SelectWhereStep<ProdutoRecord> select = database.selectFrom(PRODUTO);
				if (StringUtils.isNotBlank(term)) {
					select.where(PRODUTO.DESCRICAO.likeIgnoreCase("%" + term + "%"))
						.or(PRODUTO.CODIGO.likeIgnoreCase(term + "%"));
				}
				final Result<ProdutoRecord> resultRecord = select.fetch();
				for (ProdutoRecord record : resultRecord) {
					produtos.add(buildProduto(record));
				}
				return produtos;
			}
			
		}.execute();
	}
	
	@GET
	@Path("/produtos/estocaveis")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Produto> listProdutosEstocaveis(final @QueryParam("term") String term) throws Exception {
		return new Transaction<List<Produto>>() {
			@Override
			protected List<Produto> execute(DSLContext database) {
				final ArrayList<Produto> produtos = new ArrayList<Produto>();
				final SelectWhereStep<ProdutoRecord> select = database.selectFrom(PRODUTO);
				if (StringUtils.isNotBlank(term)) {
					select.where(PRODUTO.DESCRICAO.likeIgnoreCase("%" + term + "%"))
						.or(PRODUTO.CODIGO.likeIgnoreCase(term + "%"))
						.and(PRODUTO.ESTOCAVEL.eq("S"));
				}
				final Result<ProdutoRecord> resultRecord = select.fetch();
				for (ProdutoRecord record : resultRecord) {
					produtos.add(buildProduto(record));
				}
				return produtos;
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
			protected Categoria execute(DSLContext database) {
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
				
				final Result<ComissaoRecord> recordsComissao = database
						.selectFrom(COMISSAO)
						.where(COMISSAO.CATEGORIA_ID.eq(categoriaRecord.getId()))
						.fetch();
				
				for (ComissaoRecord recordComissao : recordsComissao) {
					final Comissao comissao = new Comissao();
					comissao.setParte(Parte.valueOf(recordComissao.getValue(COMISSAO.PARTE)).getValue());
					comissao.setPorcentagem(recordComissao.getValue(COMISSAO.PORCENTAGEM));
					categoria.getComissoes().add(comissao);
				}
				
				return categoria;
			}

		}.execute();
	}
	
	private Produto buildProduto(ProdutoRecord produtoRecord) {
		final Produto produto = new Produto();
		produto.setId(produtoRecord.getId());
		produto.setCodigo(produtoRecord.getCodigo());
		produto.setDescricao(produtoRecord.getDescricao());
		produto.setCusto(produtoRecord.getCusto());
		produto.setPreco(produtoRecord.getPreco());
		produto.setEstocavel("S".equals(produtoRecord.getEstocavel()));
		return produto;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Categoria create(final Categoria categoria) throws Exception {
		final Long categoriaId = new Transaction<Long>(true) {
			@Override
			public Long execute(DSLContext database) {
				final CategoriaRecord categoriaRecord = database.insertInto(
							CATEGORIA, 
							CATEGORIA.DESCRICAO
						)
						.values(trimToNull(categoria.getDescricao()))
						.returning(CATEGORIA.ID)
						.fetchOne();
				
				final Long id = categoriaRecord.getId();
				for (Produto produto : categoria.getProdutos()) {
					database.insertInto(PRODUTO, 
								PRODUTO.CATEGORIA_ID, 
								PRODUTO.CODIGO, 
								PRODUTO.DESCRICAO, 
								PRODUTO.CUSTO, 
								PRODUTO.PRECO,
								PRODUTO.ESTOCAVEL
							)
							.values(
									id, 
									trimToNull(produto.getCodigo()), 
									trimToNull(produto.getDescricao()), 
									produto.getCusto(), 
									produto.getPreco(),
									produto.isEstocavel() ? "S" : "N"
							)
							.execute();
				}
				for (Comissao comissao : categoria.getComissoes()) {
					final Parte parte = Parte.fromValue(comissao.getParte());
					database.insertInto(COMISSAO, 
								COMISSAO.CATEGORIA_ID, 
								COMISSAO.PARTE, 
								COMISSAO.PORCENTAGEM
							)
							.values(
									id, 
									parte.name(), 
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
			public Void execute(DSLContext database) {
				database.update(CATEGORIA)
						.set(CATEGORIA.DESCRICAO, trimToNull(categoria.getDescricao()))
						.where(CATEGORIA.ID.eq(id))
						.execute();
				
				final List<Long> produtoIds = new ArrayList<>(categoria.getProdutos().size());
				for (Produto produto : categoria.getProdutos()) {
					if (produto.getId() == null) {
						final ProdutoRecord produtoRecord = database.insertInto(PRODUTO, 
									PRODUTO.CATEGORIA_ID, 
									PRODUTO.CODIGO, 
									PRODUTO.DESCRICAO, 
									PRODUTO.CUSTO, 
									PRODUTO.PRECO,
									PRODUTO.ESTOCAVEL
								)
								.values(
										id, 
										trimToNull(produto.getCodigo()), 
										trimToNull(produto.getDescricao()), 
										produto.getCusto(), 
										produto.getPreco(),
										produto.isEstocavel() ? "S" : "N")
								.returning(PRODUTO.ID)
								.fetchOne();
						produto.setId(produtoRecord.getId());
					} else {
						database.update(PRODUTO)
							.set(PRODUTO.CATEGORIA_ID, id)
							.set(PRODUTO.CODIGO, trimToNull(produto.getCodigo()))
							.set(PRODUTO.DESCRICAO, trimToNull(produto.getDescricao()))
							.set(PRODUTO.CUSTO, produto.getCusto())
							.set(PRODUTO.PRECO, produto.getPreco())
							.set(PRODUTO.ESTOCAVEL, produto.isEstocavel() ? "S" : "N")
							.where(PRODUTO.ID.eq(produto.getId()))
							.execute();
					}
					produtoIds.add(produto.getId());
				}
				
				final DeleteConditionStep<ProdutoRecord> delete = database.delete(PRODUTO)
						.where(PRODUTO.CATEGORIA_ID.eq(id));
				if (!produtoIds.isEmpty()) {
					delete.and(PRODUTO.ID.notIn(produtoIds));
				}
				delete.execute();
				
				database.delete(COMISSAO)
						.where(COMISSAO.CATEGORIA_ID.eq(id))
						.execute();
				
				for (Comissao comissao : categoria.getComissoes()) {
					final Parte parte = Parte.fromValue(comissao.getParte());					
					database.insertInto(COMISSAO, 
								COMISSAO.CATEGORIA_ID, 
								COMISSAO.PARTE, 
								COMISSAO.PORCENTAGEM
							)
							.values(
									id, 
									parte.name(), 
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
			protected Void execute(DSLContext database) {
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
	
	@Data
	private static class Categoria {
		private Long id;
		private String descricao;
		private List<Produto> produtos = new ArrayList<>();
		private List<Comissao> comissoes = new ArrayList<>();
	}
	
	@Data
	private static class Produto {
		private Long id;
		private String codigo;
		private String descricao;
		private BigDecimal custo = BigDecimal.ZERO;
		private BigDecimal preco = BigDecimal.ZERO;
		private boolean estocavel;
	}
	
	@Data
	private static class Comissao {
		private String parte;
		private BigDecimal porcentagem = BigDecimal.ZERO;
	}

}
