$(function() {
	$('#action\\:adicionar-produto').button().click(function(e) {
		e.preventDefault();
		setProdutoForm({});
		showPanelProduto();
	});
	$('#action\\:cancelar-produto').button().click(function(e) {
		e.preventDefault();
		hidePanelProduto();
	});
	var trProdutoEdit = null;
	$('#action\\:confirmar-produto').button().click(function(e) {
		e.preventDefault();
		if (trProdutoEdit) {
			trProdutoEdit.empty();
			setProduto(trProdutoEdit, getProdutoForm());
			trProdutoEdit = null;
		} else {
			addProduto(getProdutoForm());
		}
		setProdutoForm({});
		hidePanelProduto();
	});
	$('#action\\:cancelar-categoria').button().click(function(e) {
		
	});
	$('#action\\:confirmar-categoria').button().disable().click(function(e) {
		e.preventDefault();
		
		var action = $.urlParam('action');
		switch(action) {
		case 'create':
			var request = $.ajax('/rest/categorias', {
				type: 'POST',
				contentType : 'application/json',
				data: JSON.stringify(getCategoriaForm())
			});
			
			request.done(function() {
				window.location = '/categorias.xhtml?action=create&status=success';
			});
			
			request.fail(function(jqXHR, textStatus) {
				window.noty({text: 'Falha ao persistir o produto', type: 'error'});
			});
			
			break;
		case 'update':
			var request = $.ajax('/rest/categorias/' + $.urlParam('categoria'), {
				type: 'PUT',
				contentType : 'application/json',
				data: JSON.stringify(getCategoriaForm())
			});
			
			request.done(function() {
				window.location = '/categorias.xhtml?action=update&status=success';
			});
			
			request.fail(function(jqXHR, textStatus) {
				window.noty({text: 'Falha ao atualizar o produto', type: 'error'});
			});
			
			break;
		}
	});

	function getCategoriaForm() {
		return {
			descricao: $('#input\\:descricao-categoria').val(),
			produtos: getProdutosForm(),
			comissoes: getComissoesForm()
		};
	}

	function getProdutosForm() {
		var produtos = [];
		$('#table\\:produtos > tbody > tr').each(function(index, tr) {
			produtos.push($(tr).data('produto'));
		});
		return produtos;
	}
	
	function getComissoesForm() {
		var comissoes = [];
		$('#list\\:partes > p').each(function(index, p) {
			comissoes.push({
				parte: $(p).children('label').text(),
				porcentagem: $(p).children('input').val()
			});
		});
		return comissoes;
	}

	function  showPanelProduto() {
		$('#action\\:adicionar-produto').hide();
		$('#panel\\:produto').show();
		$('#input\\:codigo-produto').focus();
	}

	function hidePanelProduto() {
		$('#panel\\:produto').hide();
		$('#action\\:adicionar-produto').show();
	}

	function load(categoria) {
		$('#input\\:descricao-categoria').focus().val(categoria.descricao);

		$('#table\\:produtos > tbody').empty();
		$.each(categoria.produtos, function(index, produto) {
			addProduto(produto);
		});
		var listPartes = $('#list\\:partes');
		$.each(categoria.comissoes, function(index, comissao) {
			var p = $('<p/>').appendTo(listPartes);
			$('<label/>', {
				'class': 'label',
				text: comissao.parte
			}).appendTo(p);
			$('<input/>', {
				id: 'list:partes:'+ index +':input:porcentagem-parte', 
				'class': 'valuebox', 
				type: 'text', 
				value: comissao.porcentagem
			}).appendTo(p);
		});
		
		$('#action\\:confirmar-categoria').enable();
	}

	function addProduto(produto) {
		var tableProdutos = $('#table\\:produtos > tbody');
		var tr = $('<tr/>').appendTo(tableProdutos);
		setProduto(tr, produto);
	}

	function setProduto(tr, produto) {
		tr.data('produto', produto);
		
		$('<td/>', {text: produto.codigo}).appendTo(tr);
		$('<td/>', {text: produto.descricao}).appendTo(tr);
		$('<td/>', {text: produto.custo}).appendTo(tr);
		$('<td/>', {text: produto.preco}).appendTo(tr);
		var tdEdit = $('<td/>').appendTo(tr);
		$('<button/>', {text: 'Editar'}).button().click(function(e) {
			e.preventDefault();
			trProdutoEdit = tr;
			setProdutoForm(produto);
			showPanelProduto();
		}).appendTo(tdEdit);
		var tdDel = $('<td/>').appendTo(tr);
		$('<button/>', {text: 'Excluir'}).button().click(function(e) {
			e.preventDefault();
			hidePanelProduto();
			tr.remove();
		}).appendTo(tdDel);
	}

	function getProdutoForm() {
		return {
			codigo: $('#input\\:codigo-produto').val(),
			descricao: $('#input\\:descricao-produto').val(),
			custo: $('#input\\:custo-produto').val(),
			preco: $('#input\\:preco-produto').val()
		};
	}

	function setProdutoForm(produto) {
		$('#input\\:codigo-produto').val(produto.codigo || null);
		$('#input\\:descricao-produto').val(produto.descricao || null);
		$('#input\\:custo-produto').val(produto.custo || 0);
		$('#input\\:preco-produto').val(produto.preco || 0);
	}

	function loadURL(url) {
		var request = $.ajax(url, {
			type:'GET'
		});
		 
		request.done(function(element) {
			load(element);
		});

		request.fail(function(jqXHR, textStatus) {
			window.noty({text: 'Falha ao carregar o produto', type: 'error'});
		});
	}

	var action = $.urlParam('action');
	switch(action) {
	case 'create':
		loadURL('/rest/categorias/new');
		break;
	case 'update':
		loadURL('/rest/categorias/' + $.urlParam('categoria'));
		break;
	}

	$('#table\\:produtos').table({
		headers: [
			{text: 'Código'},
			{text: 'Descrição'},
			{text: 'Custo'},
			{text: 'Preço'},
			{text: 'Ações', colspan: 2}
		]
	});
});