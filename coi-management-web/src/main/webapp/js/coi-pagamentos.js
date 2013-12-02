"use strict";

COI.module("Pagamentos", function(Module, COI, Backbone, Marionette, $, _) {
	
	var Pagamento = Backbone.Model.extend({
		defaults: {
			vencimento: null,
			categoria: null,
			descricao: null,
			valor: null
		},
		parse: function(resp, options) {
			resp.vencimento = $.datepicker.formatDate('dd/mm/yy', $.datepicker.parseDate('yy-mm-dd', resp.vencimento));
			return resp;
		}
	});

	var Pagamentos = Backbone.Paginator.requestPager.extend({
		url: '/rest/pagamentos',
		model: Pagamento,
		paginator_core: {
			type: 'GET',
			dataType: 'json',
			url: '/rest/pagamentos'
		},
		paginator_ui: {
			firstPage: 0,
			currentPage: 0
		},
		server_api: {
			'page' : function() {
				return this.currentPage;
			}
		}
	});
	
	var RowView = COI.ActionRowView.extend({
		template: '#pagamento_row_template',
		onUpdate: function(e) {
			Backbone.history.navigate('pagamento/' + this.model.get('id'), true);
		},
		onDelete: function(e) {
			_promptDelete(function() {
				e.model.destroy({
					wait: true,
					success: function(model, response, options) {
						_notifyDelete();
					},
					error: function(model, xhr, options) {
						_notifyDeleteFailure();
					}
				});
			});
		}
	});
	
	var FilterView = Backbone.View.extend({
		initialize: function() {
			_.bindAll(this);
		},
		render: function() {
			var that = this;
			
			{
				var div = $('<dl/>', {'class': 'coi-form-item'});
				this.$el.append(div);
				
				div.append($('<dt/>', {text: 'Situação:', 'for': 'situacao'}));
				this.situacao = $('<select/>' , {
					id: 'situacao',
					change: this.search
				})
					.append($('<option/>', {text: 'Pendente', selected: 'selected'}))
					.append($('<option/>', {text: 'Pago'}));
				div.append($('<dd/>').append(this.situacao));
				this.situacao.input();
			}
			
			{
				var div = $('<dl/>', {'class': 'coi-form-item'});
				this.$el.append(div);
				
				div.append($('<dt/>', {text: 'Data Inicial:', 'for': 'data-inicial'}));
				this.startDate = $('<input/>' , {
					type: 'text',
					id: 'data-inicial',
					'class': 'coi-input-search',
					change: function() {
						that._format($(this));
						that.search();
					}
				});
				this.startDate.val($.datepicker.formatDate('dd/mm/yy', new Date(new Date().setDate(1))));
				div.append($('<dd/>').append(this.startDate));
				this.startDate.input();
			}
			
			{
				var div = $('<dl/>', {'class': 'coi-form-item'});
				this.$el.append(div);
				
				div.append($('<dt/>', {text: 'Data Final:', 'for': 'data-final'}));
				this.endDate = $('<input/>' , {
					type: 'text',
					id: 'data-final',
					'class': 'coi-input-search',
					change: function() {
						that._format($(this));
						that.search();
					}
				});
				this.endDate.val($.datepicker.formatDate('dd/mm/yy', new Date(new Date(new Date().setMonth(new Date().getMonth() + 1)).setDate(0))));
				div.append($('<dt/>').append(this.endDate));
				this.endDate.input();
			}
			
			this.$el
				.append($('<button/>' , {text: 'Anterior', 'class': 'coi-action-prev', click: this.prev}).button())
				.append($('<button/>' , {text: 'Próxima', 'class': 'coi-action-prox', click: this.next}).button());
			
			this.search();
		},
		_format: function($input) {
			$input.val($.datepicker.formatDate('dd/mm/yy', this._parse($input)));
		},
		_parse: function($input) {
			return $.datepicker.parseDate('ddmmyy', $input.val().replace(/\//g, ''));
		},
		_send: function($input) {
			return $.datepicker.formatDate('yy-mm-dd', this._parse($input));
		},
		search: function() {
			this.collection.fetch({data: {situacao: this.situacao.val(), start: this._send(this.startDate), end: this._send(this.endDate)}});
		},
		prev: function(e) {
			if (e && e.preventDefault){ e.preventDefault(); }
	        if (e && e.stopPropagation){ e.stopPropagation(); }
			this.collection.prevPage({data: {situacao: this.situacao.val(), start: this._send(this.startDate), end: this._send(this.endDate)}});
		},
		next: function(e) {
			if (e && e.preventDefault){ e.preventDefault(); }
	        if (e && e.stopPropagation){ e.stopPropagation(); }
			this.collection.nextPage({data: {situacao: this.situacao.val(), start: this._send(this.startDate), end: this._send(this.endDate)}});
		}
	});
	
	var View = COI.GridView.extend({
		searchView: FilterView,
		itemView: RowView,
		templateHelpers: {
			header: 'Pagamentos',
			columns: {
				data: 'Vencimento',
				categoria: 'Categoria',
				descricao: 'Descrição',
				valor: 'Valor'
			}
		},
		extras: {
			'categorias': {
				text: 'Categorias',
				trigger: 'categorias'
			},
			'impressao': {
				text: 'Impressão',
				trigger: 'impressao'
			}
		},
		initialize: function() {
			//this.collection.fetch();
		},
		onCancel: function(e) {
			Backbone.history.navigate('', true);
		},
		onCreate: function(e) {
			Backbone.history.navigate('pagamento', true);
		},
		onCategorias: function(e) {
			Backbone.history.navigate('pagamento-categorias', true);
		},
		onImpressao: function(e) {
			var startDate = this._search._send(this._search.startDate);
			var endDate = this._search._send(this._search.endDate);
			this.$el.append($('<iframe/>', {'src': '/rest/pagamentos/imprimir?situacao=' + this._search.situacao.val() + '&start=' + startDate + '&end=' + endDate}).hide());
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('pagamentos', function() {
			COI.body.show(new View({collection: new Pagamentos()}));
		});
	});
});