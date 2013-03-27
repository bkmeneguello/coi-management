$(function() {
	switch($.urlParam('action')) {
	case 'create':
		switch($.urlParam('status')) {
		case 'success':
			window.noty({text: 'Produto incluido com sucesso', type: 'success', timeout: 2000});
		}
		break;
	case 'update':
		switch($.urlParam('status')) {
		case 'success':
			window.noty({text: 'Produto alterado com sucesso', type: 'success', timeout: 2000});
		}
		break;
	case 'delete':
		switch($.urlParam('status')) {
		case 'success':
			window.noty({text: 'Produto excluido com sucesso', type: 'success', timeout: 2000});
		}
		break;
	}
	
	$('#action\\:adicionar-categoria').button();

	$('#table\\:categorias').dataTable('/rest/categorias', {
		headers: [
			{
				text: 'Descriçao',
				property: 'descricao'
			},
			{
				text: 'Ações',
				renderer: function(tr, element, property) {
					var td = $('<td/>').appendTo(tr);
					$('<button/>', {text: 'Editar'}).button().click(function(e) {
						e.preventDefault();
						window.location = '/categoria.xhtml?action=update&categoria=' + encodeURIComponent(element.descricao);
					}).appendTo(td);
					$('<button/>', {text: 'Excluir'}).button().click(function(e) {
						e.preventDefault();
						noty({
							text: 'Deseja realmente excluir a categoria?',
							layout: 'center',
							buttons: [
							    {
								    addClass: 'btn',
								    text: 'Cancelar',
								    onClick: function($noty) {
								        $noty.close();
							    	}
							 	},
								{
									addClass: 'btn', 
									text: 'Confirmar', 
									onClick: function($noty) {
										var request = $.ajax('/rest/categorias/' + encodeURIComponent(element.descricao), {
											type:'DELETE'
										});
										 
										request.done(function(element) {
									        $noty.close();
									        window.location = '/categorias.xhtml?action=delete&status=success';
										});

										request.fail(function(jqXHR, textStatus) {
											$noty.close();
									        noty({text: 'Falha na exclusão da categoria', type: 'error'});
										});
							      	}
							    }
							],
							callback: {
								onShow: function() {
									$('button.btn').button();
								}
							}
						});
					}).appendTo(td);
				}
			}
		]
	});
});